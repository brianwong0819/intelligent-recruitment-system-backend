#!/usr/bin/env python3
import google.generativeai as genai
import json
import logging
import os
import argparse
from datetime import datetime

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f"training_quiz_{datetime.now().strftime('%Y%m%d')}.log"),
        logging.StreamHandler()    ]
)
logger = logging.getLogger("training_quiz")

# Model names to try in order of preference
MODEL_NAMES = [
    "models/gemini-2.0-flash",
    "models/gemini-1.5-pro",
    "models/gemini-1.5-flash"
]

def configure_genai_api(api_key):
    """Configure the Gemini AI API with the provided key."""
    try:
        genai.configure(api_key=api_key)

        logger.info("Gemini AI API configured successfully")
        return True
    except Exception as e:
        logger.error(f"Failed to configure Gemini AI API: {str(e)}")
        return False

def get_pdf_path(pdf_url, base_dir="."):
    """
    Convert the PDF URL to a local file path.

    Args:
        pdf_url: The URL path of the PDF from the API
        base_dir: The base directory of the application

    Returns:
        The full path to the PDF file
    """
    # Check if pdf_url is null or doesn't start with expected prefix
    if not pdf_url or not pdf_url.startswith('/api/training/materials/'):
        return None

    # Extract the filename part from the URL
    filename = pdf_url.split('/')[-1]

    # Construct the full path relative to the base directory
    pdf_path = os.path.join(base_dir, 'uploads', 'training-materials', filename)

    # Check if the file exists
    if not os.path.exists(pdf_path):
        logger.warning(f"Training PDF file not found at path: {pdf_path}")
        return None

    logger.info(f"Found training PDF file at: {pdf_path}")
    return pdf_path

def prepare_quiz_prompt(job_title, job_description=None):
    """Prepare the prompt for Gemini AI to generate quiz questions."""
    prompt = f"""
You are an AI training quiz generator for an event recruitment platform. Your task is to analyze the attached PDF training material for a "{job_title}" position and create 10 multiple-choice questions that test critical knowledge from the material.

IMPORTANT INSTRUCTIONS:
1. ONLY create questions based on CONTENT EXPLICITLY FOUND in the training slides.
2. DO NOT generate questions about content not explicitly covered in the slides.
3. Focus on critical operational details such as:
   - Product features and specifications
   - Promotional offers and how to obtain them
   - Job responsibilities and duties
   - Proper procedures and protocols
   - Customer interaction guidelines
   - Event-specific information
4. Questions must focus on practical knowledge that the promoter needs to do their job effectively.
5. Avoid trivial questions about dates, slide numbers, or other non-essential details.
6. DO NOT start questions with phrases like "According to the briefing deck..." or "According to slide X...".
7. Ask questions directly, such as "What is the exclusive welcome pack available for new Luckin Coffee Malaysia app users?" instead of "According to slide 6, what exclusive welcome pack...".

QUESTION FORMAT REQUIREMENTS:
- Generate exactly 10 multiple-choice questions.
- Each question must have exactly 4 options (A, B, C, D).
- Only one option can be correct.
- Questions should test practical knowledge needed for the job.
- Include a clear reference to the specific slide number and section for each question.
- Provide a brief explanation for why the correct answer is correct.
- Questions must be diverse and cover different aspects of the training material.

RETURN EXACTLY THIS JSON FORMAT AND NOTHING ELSE:
{{
  "questions": [
    {{
      "id": 1,
      "question": "Clear and concise question text?",
      "options": {{
        "A": "First option",
        "B": "Second option",
        "C": "Third option",
        "D": "Fourth option"
      }},
      "correct_answer": "A",
      "explanation": "Brief explanation of why A is correct",
      "reference": "Slide 5, Section: Product Features"
    }},
    ...
  ]
}}

VERIFICATION CHECKLIST:
- Each question must directly reference information from the slides
- All questions must be job-relevant and test practical knowledge needed during work
- References must be specific (slide number and section)
- Questions must be clearly written and unambiguous
- The correct answer must be clearly defensible based on the training material
- Formatting must strictly follow the JSON template provided
- Questions should directly ask about information without prefacing with "According to..."
- No questions about general dates or other trivial information that doesn't impact job performance

EXTREMELY IMPORTANT: Return ONLY valid JSON without any additional text, markdown formatting, or explanations before or after the JSON. The response MUST be parseable by a JSON parser without any modifications.
"""

    # Add job description context if available
    if job_description:
        prompt += f"\n\nADDITIONAL CONTEXT ABOUT THE POSITION:\n{job_description}\n\nUse this context to help guide the relevance of your questions, but remember that all question content MUST come from the training slides."

    return prompt

def try_models_sequentially(prompt, pdf_file):
    """
    Try multiple model names sequentially until one works.

    Args:
        prompt: The prompt to send to the model
        pdf_file: The PDF file object

    Returns:
        The model's response or None if all models fail
    """
    last_error = None

    for model_name in MODEL_NAMES:
        try:
            logger.info(f"Trying model: {model_name}")
            model = genai.GenerativeModel(model_name=model_name)

            # Send request with PDF and prompt
            logger.info(f"Sending request with PDF to model: {model_name}")
            response = model.generate_content([pdf_file, prompt])

            logger.info(f"Successfully used model: {model_name}")
            return response
        except Exception as e:
            logger.warning(f"Failed to use model {model_name}: {str(e)}")
            last_error = e

    # If we get here, all models failed
    raise Exception(f"All models failed. Last error: {str(last_error)}")

def generate_quiz(api_key, pdf_url, job_title, job_description=None, base_dir="."):
    """
    Generate a quiz using Gemini AI based on training PDF.

    Args:
        api_key: The Gemini AI API key
        pdf_url: The URL of the training PDF file
        job_title: The title of the job
        job_description: Optional job description for context
        base_dir: Base directory for finding PDF files

    Returns:
        Dictionary containing quiz questions and answers
    """
    # Configure the API
    if not configure_genai_api(api_key):
        return {"error": "Failed to configure API"}

    try:
        # Get the full path to the PDF
        pdf_path = get_pdf_path(pdf_url, base_dir)

        if not pdf_path:
            return {"error": "Training PDF file not found"}

        # Upload the PDF to Gemini
        try:
            logger.info(f"Uploading PDF from path: {pdf_path}")
            pdf_file = genai.upload_file(path=pdf_path)
            logger.info(f"Successfully uploaded PDF: {pdf_file.name}")
        except Exception as e:
            logger.error(f"Failed to upload PDF: {str(e)}")
            return {"error": f"Failed to upload PDF: {str(e)}"}

        # Prepare the prompt
        prompt = prepare_quiz_prompt(job_title, job_description)
        logger.info("Prepared quiz generation prompt")

        # Try all models sequentially
        logger.info("Sending request to Gemini AI...")
        response = try_models_sequentially(prompt, pdf_file)
        logger.info("Received response from Gemini AI")

        # Extract and parse JSON from response
        try:
            # Try to parse the response text as JSON
            result = json.loads(response.text)
            logger.info("Successfully parsed response as JSON")
            return result
        except json.JSONDecodeError as e:
            # If parsing fails, try to extract JSON from the text
            logger.warning(f"Failed to parse response as JSON: {str(e)}")
            logger.warning("Attempting to extract JSON from text...")

            # Find JSON object in text (between curly braces)
            response_text = response.text
            start_idx = response_text.find('{')
            end_idx = response_text.rfind('}') + 1

            if start_idx >= 0 and end_idx > start_idx:
                json_str = response_text[start_idx:end_idx]
                try:
                    result = json.loads(json_str)
                    logger.info("Successfully extracted and parsed JSON from text")
                    return result
                except json.JSONDecodeError:
                    logger.error("Failed to extract valid JSON from response")

            # Return error with full response for debugging
            return {
                "error": "Failed to parse AI response as JSON",
                "rawResponse": response.text
            }

    except Exception as e:
        logger.error(f"Error during quiz generation: {str(e)}")
        return {"error": f"Quiz generation failed: {str(e)}"}

def main():
    """Main function to handle command line arguments and process quiz generation."""
    parser = argparse.ArgumentParser(description='Generate quiz questions from training PDF using Gemini AI')
    parser.add_argument('--api-key', required=True, help='Gemini AI API key')
    parser.add_argument('--pdf-url', required=True, help='URL path to the training PDF')
    parser.add_argument('--job-title', required=True, help='Title of the job')
    parser.add_argument('--job-description', help='Optional job description for context')
    parser.add_argument('--output', help='Output file for quiz results (default: stdout)')
    parser.add_argument('--base-dir', help='Base directory for finding PDF files', default='.')

    args = parser.parse_args()

    try:
        # Generate the quiz
        result = generate_quiz(
            args.api_key,
            args.pdf_url,
            args.job_title,
            args.job_description,
            args.base_dir
        )

        # Write output
        if args.output:
            logger.info(f"Writing quiz results to {args.output}")
            with open(args.output, 'w', encoding='utf-8') as f:
                json.dump(result, f, indent=2)
        else:
            logger.info("Writing quiz results to stdout")
            print(json.dumps(result, indent=2))

    except Exception as e:
        logger.error(f"Error in main function: {str(e)}")
        print(json.dumps({"error": f"Quiz generation failed: {str(e)}"}))

if __name__ == "__main__":
    main()