#!/usr/bin/env python3
import google.generativeai as genai
import json
import sys
import os
import argparse
import logging
from datetime import datetime

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f"candidate_evaluation_{datetime.now().strftime('%Y%m%d')}.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("candidate_evaluation")

# Model names to try in order of preference
MODEL_NAMES = [
    "models/gemini-2.0-flash",
    "models/gemini-1.5-pro",
    "gemini-1.5-pro",
    "gemini-pro"
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

def get_full_resume_path(resume_url, base_dir="."):
    """
    Convert the resume URL to a local file path.

    Args:
        resume_url: The URL path of the resume from the API
        base_dir: The base directory of the application

    Returns:
        The full path to the resume file
    """
    # Handle cases where resumeUrl is null or doesn't start with /api/files/
    if not resume_url or not resume_url.startswith('/api/files/resumes/'):
        return None

    # Extract the filename part from the URL
    filename = resume_url.split('/')[-1]

    # Construct the full path relative to the base directory
    resume_path = os.path.join(base_dir, 'uploads', 'resumes', filename)

    # Check if the file exists
    if not os.path.exists(resume_path):
        logger.warning(f"Resume file not found at path: {resume_path}")
        return None

    logger.info(f"Found resume file at: {resume_path}")
    return resume_path

def prepare_evaluation_prompt(candidate_data, resume_file=None):
    """
    Prepare the prompt for Gemini AI based on candidate data.

    Args:
        candidate_data: The candidate data dictionary
        resume_file: The uploaded resume file object or None

    Returns:
        The prompt text
    """
    # Create a structured prompt for the AI
    prompt = f"""
You are an expert AI recruitment assistant for an event staffing company. Your task is to evaluate a candidate's suitability for a job position based on the following data and resume:

JOB DETAILS:
- Job Title: {candidate_data.get('jobTitle', 'N/A')}
- Job Type: {candidate_data.get('jobTitleType', 'N/A')}
- Job Requirements: {candidate_data.get('jobRequirements', 'N/A')}
- Job Scope: {candidate_data.get('jobScope', 'N/A')}
- Work Dates: {', '.join(candidate_data.get('appliedWorkDates', ['N/A']))}
- Total Work Days: {candidate_data.get('totalWorkDays', 'N/A')}
- Locations: {', '.join(candidate_data.get('locationNames', ['N/A']))}

CANDIDATE DETAILS:
- Name: {candidate_data.get('candidateName', 'N/A')}
- Gender: {candidate_data.get('gender', 'N/A')}
- Employment Status: {candidate_data.get('employmentStatus', 'N/A')}
- Languages: {', '.join(candidate_data.get('languages', ['N/A']))}
- Bio: {candidate_data.get('bio', 'N/A')}

EXPERIENCE:
{format_experience(candidate_data.get('experiences', []))}

AVAILABILITY:
- Availability Type: {candidate_data.get('availabilityType', 'N/A')}
- Available Dates: {', '.join(candidate_data.get('availableDates', ['N/A']))}

LOCATION:
- Distance to Job: {candidate_data.get('distanceToCandidate', 'N/A')} km

"""

    # Add resume note to the prompt
    if resume_file is None:
        prompt += "NO RESUME AVAILABLE: The candidate has not provided a resume.\n\n"
    else:
        prompt += "RESUME: I've been provided with the candidate's resume as a PDF file which I'll analyze.\n\n"

    prompt += """
Using all the above information, please evaluate this candidate and provide scores on a scale of 1.0 to 10.0 in the following categories:
1. Experience Score: How well the candidate's experience matches the job requirements
2. Skills Score: The candidate's overall skills and capabilities for this role
3. Location Score: Convenience of the job location for the candidate
4. Availability Score: How well the candidate's availability matches the job schedule
5. Resume Score: The strength of the candidate's overall profile and resume quality
6. Reputation Score: Assumed reputation based on experience and profile (default to 7.5 if unclear)

Then calculate:
- AI Model Score: Your assessment of the overall fit (weighted average)
- Final Score: The final recommendation score

Also provide a detailed feedback paragraph explaining your evaluation.

Return ONLY a JSON object with the following structure:
{
  "experienceScore": (float between 1.0-10.0),
  "skillsScore": (float between 1.0-10.0),
  "locationScore": (float between 1.0-10.0),
  "availabilityScore": (float between 1.0-10.0),
  "resumeScore": (float between 1.0-10.0),
  "reputationScore": (float between 1.0-10.0),
  "aiModelScore": (float between 1.0-10.0),
  "finalScore": (float between 1.0-10.0),
  "feedback": (detailed explanation of your evaluation)
}

DO NOT include any other text before or after the JSON.
"""
    return prompt

def format_experience(experiences):
    """Format the experiences list for the prompt."""
    if not experiences:
        return "- No specific experiences listed"

    formatted = []
    for idx, exp in enumerate(experiences, 1):
        job_type = exp.get('jobType', 'Not specified')
        description = exp.get('experienceText', 'No description provided')
        formatted.append(f"- Experience {idx}: {job_type} - {description}")

    return "\n".join(formatted)

def try_models_sequentially(prompt, resume_file=None):
    """
    Try multiple model names sequentially until one works.

    Args:
        prompt: The prompt to send to the model
        resume_file: The resume file object or None

    Returns:
        The model's response or None if all models fail
    """
    last_error = None

    for model_name in MODEL_NAMES:
        try:
            logger.info(f"Trying model: {model_name}")
            model = genai.GenerativeModel(model_name=model_name)

            # If we have a resume file, include it in the content
            if resume_file is not None:
                logger.info(f"Sending request with resume to model: {model_name}")
                response = model.generate_content([resume_file, prompt])
            else:
                logger.info(f"Sending request without resume to model: {model_name}")
                response = model.generate_content(prompt)

            logger.info(f"Successfully used model: {model_name}")
            return response
        except Exception as e:
            logger.warning(f"Failed to use model {model_name}: {str(e)}")
            last_error = e

    # If we get here, all models failed
    raise Exception(f"All models failed. Last error: {str(last_error)}")

def evaluate_candidate(api_key, candidate_data, base_dir="."):
    """
    Evaluate a candidate using Gemini AI.

    Args:
        api_key: The Gemini AI API key
        candidate_data: Dictionary containing candidate and job information
        base_dir: Base directory for finding resume files

    Returns:
        Dictionary containing evaluation scores and feedback
    """
    # Configure the API
    if not configure_genai_api(api_key):
        return {"error": "Failed to configure API"}

    try:
        # Check for resume
        resume_file = None
        resume_path = None
        resume_url = candidate_data.get('resumeUrl')

        if resume_url:
            # Get the full path to the resume
            resume_path = get_full_resume_path(resume_url, base_dir)

            # If resume exists, upload it to Gemini
            if resume_path:
                try:
                    logger.info(f"Uploading resume from path: {resume_path}")
                    resume_file = genai.upload_file(path=resume_path)
                    logger.info(f"Successfully uploaded resume: {resume_file.name}")
                except Exception as e:
                    logger.error(f"Failed to upload resume: {str(e)}")
                    resume_file = None

        # Prepare the prompt
        prompt = prepare_evaluation_prompt(candidate_data, resume_file)
        logger.info("Prepared evaluation prompt")

        # Try all models sequentially
        logger.info("Sending request to Gemini AI...")
        response = try_models_sequentially(prompt, resume_file)
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
        logger.error(f"Error during candidate evaluation: {str(e)}")
        return {"error": f"Evaluation failed: {str(e)}"}

def main():
    """Main function to handle command line arguments and process evaluation."""
    parser = argparse.ArgumentParser(description='Evaluate a candidate using Gemini AI')
    parser.add_argument('--api-key', required=True, help='Gemini AI API key')
    parser.add_argument('--input', required=True, help='JSON file containing candidate data or - for stdin')
    parser.add_argument('--output', help='Output file for evaluation results (default: stdout)')
    parser.add_argument('--base-dir', help='Base directory for finding resume files', default='.')

    args = parser.parse_args()

    try:
        # Read input data
        if args.input == '-':
            logger.info("Reading candidate data from stdin")
            candidate_data = json.load(sys.stdin)
        else:
            logger.info(f"Reading candidate data from {args.input}")
            with open(args.input, 'r', encoding='utf-8') as f:
                candidate_data = json.load(f)

        # Evaluate the candidate
        result = evaluate_candidate(args.api_key, candidate_data, args.base_dir)

        # Write output
        if args.output:
            logger.info(f"Writing evaluation results to {args.output}")
            with open(args.output, 'w', encoding='utf-8') as f:
                json.dump(result, f, indent=2)
        else:
            logger.info("Writing evaluation results to stdout")
            print(json.dumps(result, indent=2))

    except Exception as e:
        logger.error(f"Error in main function: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()