
import requests

OPEN_FOOD_FACTS_URL = (
    "https://world.openfoodfacts.org/api/v3/product/"
)

HEADERS = {
    "User-Agent": "NutriCheck/1.0 (college-project)"
}

FIELDS = ",".join([
    "code",
    "product_name",
    "brands",
    "categories",
    "image_front_url",
    "image_url",
    "ingredients_text",
    "ingredients_text_en",
    "ingredients_text_hi",
    "ingredients_text_with_allergens",
    "ingredients",
    "ingredients_tags",
    "ingredients_hierarchy",
    "allergens",
    "traces",
    "nutriments",
    "quantity",
    "serving_size",
    "nutrition_data_per",
])


def get_product_from_openfoodfacts(barcode: str):
    barcode = barcode.strip()

    if not barcode:
        return None

    url = f"{OPEN_FOOD_FACTS_URL}{barcode}"

    response = requests.get(
        url,
        params={"fields": FIELDS},
        headers=HEADERS,
        timeout=15,
    )

    response.raise_for_status()
    data = response.json()

    if data.get("status") != "success":
        return None

    product = data.get("product")

    if not isinstance(product, dict) or not product:
        return None

    returned_code = str(
        product.get("code") or data.get("code") or ""
    )

    if returned_code and returned_code != barcode:
        return None

    # Preserve the original API response and expose the best
    # available ingredient text through one consistent field.
    ingredient_candidates = [
        product.get("ingredients_text"),
        product.get("ingredients_text_en"),
        product.get("ingredients_text_hi"),
    ]

    ingredients_text = next(
        (
            value.strip()
            for value in ingredient_candidates
            if isinstance(value, str) and value.strip()
        ),
        "",
    )

    # Use structured ingredient names only as a fallback.
    if not ingredients_text:
        structured = product.get("ingredients")

        if isinstance(structured, list):
            names = [
                item.get("text", "").strip()
                for item in structured
                if isinstance(item, dict)
                and isinstance(item.get("text"), str)
                and item.get("text", "").strip()
            ]

            if names:
                ingredients_text = ", ".join(names)

    if ingredients_text:
        product["ingredients_text"] = ingredients_text

    return data
