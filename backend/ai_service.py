
import os

from dotenv import load_dotenv
from groq import Groq

load_dotenv()

API_KEY = os.getenv("GROQ_API_KEY")
MODEL_NAME = os.getenv("GROQ_MODEL")

if not API_KEY:
    raise RuntimeError("GROQ_API_KEY is missing from .env")

if not MODEL_NAME:
    raise RuntimeError("GROQ_MODEL is missing from .env")

client = Groq(api_key=API_KEY)


SYSTEM_PROMPT = """
You are NutriCheck AI, a concise packaged-food analyst.
Analyze the supplied product data and answer the user's question.

Use these five headings in this exact order:

**Overall Verdict**
Choose: Better choice, Okay in moderation, Use with caution,
or Insufficient information. Briefly explain why.

**Quick Analysis**
Summarize key ingredients, available nutrition facts, and concerns.

**Age Suitability**
Give cautious, relevant age guidance. Mention choking or ingredient
concerns when applicable. Never invent a minimum recommended age.

**Recommendation**
Suggest one practical alternative or label feature to compare, if useful.
Check declared allergens before suggesting alternatives.

**Allergy Warning**
Mention declared allergens and relevant uncertainty. Never guarantee
allergy safety.

Rules:
- Keep the complete response under 150 words.
- Use short sentences and bullets; never use tables.
- Use only supplied product facts. Never invent missing values.
- Mark missing nutrition data as "Not available".
- Separate label facts from general guidance.
- Do not diagnose conditions or guarantee product safety.
- If data is insufficient, state that clearly.
- Answer in simple English.
"""



def generate_food_response(
    question: str,
    product_name: str = "",
    ingredients: str = "",
    calories: str = "",
    sugar: str = "",
    fat: str = "",
    protein: str = "",
    allergens: str = "",
) -> str:

    product_context = f"""
Product: {product_name or "Not available"}
Ingredients: {ingredients or "Not available"}
Calories per 100 g: {calories or "Not available"}
Sugar per 100 g: {sugar or "Not available"}
Fat per 100 g: {fat or "Not available"}
Protein per 100 g: {protein or "Not available"}
Declared allergens: {allergens or "Not available"}
"""

    completion = client.chat.completions.create(
        model=MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": (
                    f"{product_context}\n"
                    f"User request: {question}\n\n"
                    "Return one complete report. Include all five required "
                    "headings in the specified order. Do not repeat sections "
                    "or place the conclusion before the report."
                ),
            },
        ],
        temperature=0.2,
        max_completion_tokens=700,
    )

    choice = completion.choices[0]
    answer = choice.message.content or ""

    # Log the actual reason the model stopped.
    print(
        "NutriCheck AI:",
        f"finish_reason={choice.finish_reason},",
        f"answer_length={len(answer)},",
        f"model={completion.model}",
    )

    if not answer.strip():
        raise RuntimeError("Groq returned an empty response")

    if choice.finish_reason == "length":
        print("WARNING: AI response reached its token limit")

    return answer.strip()
