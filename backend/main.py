
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from ai_service import generate_food_response

from database import get_connection
from openfoodfacts import get_product_from_openfoodfacts

app = FastAPI(title="NutriCheck API")

class FoodAssistantRequest(BaseModel):
    question: str
    product_name: str = ""
    ingredients: str = ""
    calories: str = ""
    sugar: str = ""
    fat: str = ""
    protein: str = ""
    allergens: str = ""



@app.get("/")
def home():
    return {"message": "NutriCheck API is running"}



def get_number(nutriments, *keys):
    for key in keys:
        value = nutriments.get(key)

        if value is None or value == "":
            continue

        try:
            number = float(value)

            if number >= 0:
                return number
        except (ValueError, TypeError):
            continue

    return None


def extract_nutrition(product_data):
    nutriments = product_data.get("nutriments") or {}

    calories = get_number(
        nutriments, "energy-kcal_100g"
    )

    if calories is None:
        energy_kj = get_number(
            nutriments, "energy-kj_100g"
        )

        if energy_kj is None:
            # Some products report energy_100g in kJ.
            energy_kj = get_number(
                nutriments, "energy_100g"
            )

        if energy_kj is not None:
            calories = round(energy_kj / 4.184, 2)

    return {
        "calories": calories,
        "sugar": get_number(nutriments, "sugars_100g"),
        "fat": get_number(nutriments, "fat_100g"),
        "protein": get_number(nutriments, "proteins_100g"),
    }


@app.get("/api/products/{barcode}")
def get_product(barcode: str):
    # 1. Check MySQL first.
    connection = get_connection()

    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute(
            "SELECT * FROM products WHERE barcode = %s",
            (barcode,),
        )
        existing = cursor.fetchone()
        cursor.close()
    finally:
        connection.close()

    # Return cached product only if all four nutrition values exist.
    nutrition_fields = ("calories", "sugar", "fat", "protein")

    if existing and all(
        existing.get(field) is not None
        for field in nutrition_fields
    ):
        return {"source": "database", "product": existing}

    # 2. Fetch from Open Food Facts if missing/incomplete.
    try:
        off_data = get_product_from_openfoodfacts(barcode)
    except Exception as exc:
        # If API fails but we have a cached product, return that.
        if existing:
            return {
                "source": "database",
                "product": existing,
                "warning": "Open Food Facts could not be reached.",
            }

        raise HTTPException(
            status_code=502,
            detail="Could not connect to Open Food Facts",
        ) from exc

    product_data = (
        off_data.get("product")
        if isinstance(off_data, dict)
        else None
    )

    if not isinstance(product_data, dict) or not product_data:
        if existing:
            return {"source": "database", "product": existing}

        raise HTTPException(
            status_code=404,
            detail="Product not found in Open Food Facts",
        )

    nutrition = extract_nutrition(product_data)

    # 3. Use API values when available; otherwise retain cached values.
    def choose(field):
        api_value = nutrition.get(field)
        if api_value is not None:
            return api_value
        return existing.get(field) if existing else None

    product = {
        "barcode": barcode,
        "product_name": (
            product_data.get("product_name")
            or (existing.get("product_name") if existing else None)
        ),
        "brand": (
            product_data.get("brands")
            or (existing.get("brand") if existing else None)
        ),
        "ingredients": (
            product_data.get("ingredients_text")
            or product_data.get("ingredients_text_en")
            or product_data.get("ingredients_text_hi")
            or (existing.get("ingredients") if existing else None)
        ),
        "calories": choose("calories"),
        "sugar": choose("sugar"),
        "fat": choose("fat"),
        "protein": choose("protein"),
        "allergens": (
            product_data.get("allergens")
            or (existing.get("allergens") if existing else None)
        ),
        "image_url": (
            product_data.get("image_front_url")
            or product_data.get("image_url")
            or (existing.get("image_url") if existing else None)
        ),
    }

    # 4. Save the refreshed product in MySQL.
    connection = get_connection()

    try:
        cursor = connection.cursor()

        if existing:
            cursor.execute(
                """
                UPDATE products
                SET product_name = %s,
                    brand = %s,
                    ingredients = %s,
                    calories = %s,
                    sugar = %s,
                    fat = %s,
                    protein = %s,
                    allergens = %s,
                    image_url = %s
                WHERE barcode = %s
                """,
                (
                    product["product_name"],
                    product["brand"],
                    product["ingredients"],
                    product["calories"],
                    product["sugar"],
                    product["fat"],
                    product["protein"],
                    product["allergens"],
                    product["image_url"],
                    barcode,
                ),
            )
        else:
            cursor.execute(
                """
                INSERT INTO products
                (barcode, product_name, brand, ingredients,
                 calories, sugar, fat, protein, allergens, image_url)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    product["barcode"],
                    product["product_name"],
                    product["brand"],
                    product["ingredients"],
                    product["calories"],
                    product["sugar"],
                    product["fat"],
                    product["protein"],
                    product["allergens"],
                    product["image_url"],
                ),
            )

        connection.commit()
        cursor.close()
    finally:
        connection.close()

    return {
        "source": "open_food_facts",
        "product": product,
    }


@app.post("/api/ai/food-assistant")
def food_assistant(request: FoodAssistantRequest):
    question = request.question.strip()

    if not question:
        raise HTTPException(
            status_code=400,
            detail="Question cannot be empty",
        )

    try:
        answer = generate_food_response(
            question=question,
            product_name=request.product_name,
            ingredients=request.ingredients,
            calories=request.calories,
            sugar=request.sugar,
            fat=request.fat,
            protein=request.protein,
            allergens=request.allergens,
        )

        return {
            "success": True,
            "answer": answer,
        }

    except Exception as exc:
        print(f"Food Assistant error: {type(exc).__name__}: {exc}")

        raise HTTPException(
            status_code=502,
            detail="AI service is temporarily unavailable. Please try again.",
        ) from exc
