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

def prepare_evaluation_prompt(candidate_data, resume_file=None):
    """Prepare the prompt for Gemini AI based on candidate data."""
    # Create a structured prompt for the AI
    prompt = f"""
You are an expert AI recruitment assistant for an event staffing company specializing in part-time and temporary event positions. Your task is to evaluate a candidate's suitability based on specific job requirements and candidate qualifications. Your evaluation must be:
- Objective and evidence-based, using only the provided information
- Consistent across all candidates (maintaining the same evaluation standards)
- Fair and unbiased toward all demographics
- Precisely aligned with the exact job requirements

JOB DETAILS:
- Job Title: {candidate_data.get('jobTitle', 'N/A')}
- Job Type: {candidate_data.get('jobTitleType', 'N/A')}
- Job Requirements: {candidate_data.get('jobRequirements', 'N/A')}
- Job Scope: {candidate_data.get('jobScope', 'N/A')}
- Work Dates: {', '.join(candidate_data.get('appliedWorkDates', ['N/A']))}
- Total Work Days: {candidate_data.get('totalWorkDays', 'N/A')}
- Total Job Working Days: {candidate_data.get('totalJobWorkingDays', 'N/A')}
- Locations: {', '.join(candidate_data.get('locationNames', ['N/A']))}

CANDIDATE DETAILS:
- Name: {candidate_data.get('candidateName', 'N/A')}
- Gender: {candidate_data.get('gender', 'N/A')}
- Employment Status: {candidate_data.get('employmentStatus', 'N/A')}
- Languages: {', '.join(candidate_data.get('languages', ['N/A']))}
- Bio: {candidate_data.get('bio', 'N/A')}

EXPERIENCE:
{format_experience(candidate_data.get('experiences', []))}

LOCATION:
- Distance to Job: {candidate_data.get('distanceToCandidate', 'N/A')} km
"""

    # Add resume note to the prompt
    if resume_file is None:
        prompt += "NO RESUME AVAILABLE: The candidate has not provided a resume.\n\n"
    else:
        prompt += "RESUME: I've been provided with the candidate's resume as a PDF file which I'll analyze.\n\n"

    prompt += """
EVALUATION INSTRUCTIONS:

Step 1: Analyze the job requirements thoroughly, identifying KEY requirements including:
- Essential skills and qualifications
- Experience requirements
- Demographic requirements (if explicitly stated in requirements)
- Language requirements
- Commitment requirements (check if the job requires "full commitment" or similar phrases)
- Any other critical specifications

Step 2: Evaluate the candidate against each requirement systematically.

Step 3: Assign scores on a scale of 1.0 to 10.0 in these categories:

1. Experience Score (1.0-10.0):
   - Direct match between candidate's experience and job requirements
   - Score 1.0-3.9: Minimal relevant experience
   - Score 4.0-6.9: Some relevant experience but gaps in key areas
   - Score 7.0-8.9: Good match with most experience requirements
   - Score 9.0-10.0: Excellent match with all experience requirements

2. Skills Score (1.0-10.0):
   - Overall capabilities for this specific role
   - Score 1.0-3.9: Few relevant skills
   - Score 4.0-6.9: Some relevant skills but missing important ones
   - Score 7.0-8.9: Good match with most skill requirements
   - Score 9.0-10.0: Excellent match with all skill requirements

3. Resume Score (1.0-10.0):
   - Overall profile strength relative to position
   - Score 1.0-3.9: Weak profile for this role
   - Score 4.0-6.9: Moderate fit with position
   - Score 7.0-8.9: Strong profile for this role
   - Score 9.0-10.0: Exceptional profile for this role

4. AI Model Score (1.0-10.0):
   - Base score: Weighted average of above scores
   - IMPORTANT ADJUSTMENTS:
     - Full Commitment Check: If job requirements mention "prefer full commitment" or similar phrases:
       - Add 1.5 points if candidate is applying for ALL available work days (totalWorkDays = totalJobWorkingDays)
       - Subtract 1.5 points if candidate is NOT applying for all available work days
     - Bio Relevance: If candidate's bio mentions specific elements that match job scope or requirements, add 0.5 points
     - Any disqualifying factors (e.g., inability to meet explicit requirements like gender or language)
   - If there are explicit requirements the candidate doesn't meet, the AI Model Score should be substantially lower to reflect this

Step 4: CRITICAL - Review each score for fairness and consistency:
- Re-examine your reasoning for each score
- Ensure scores accurately reflect the match between requirements and qualifications
- Check that you haven't introduced any unconscious bias
- Verify scores are aligned with the scoring guidance

Return ONLY a JSON object with the following structure:
{
  "experienceScore": (float between 1.0-10.0),
  "skillsScore": (float between 1.0-10.0),
  "resumeScore": (float between 1.0-10.0),
  "aiModelScore": (float between 1.0-10.0),
  "feedback": "Your comprehensive feedback string with multiple sections separated by \\n\\n"
}

Your feedback should include these sections, clearly labeled and separated by blank lines (\\n\\n):
1. KEY REQUIREMENTS: List of key job requirements identified
2. EXPERIENCE ANALYSIS: Detailed explanation of experience score with specific examples
3. SKILLS ANALYSIS: Detailed explanation of skills score with specific examples
4. RESUME ANALYSIS: Detailed explanation of resume score with specific examples
5. COMMITMENT ANALYSIS: Analysis of candidate's commitment level relative to job requirements
6. BIO RELEVANCE: Analysis of how candidate's bio relates to job requirements (if relevant)
7. KEY STRENGTHS: 3-5 key strengths for this position
8. DEVELOPMENT AREAS: 3-5 areas of potential improvement
9. OVERALL ASSESSMENT: Final comprehensive evaluation

Within each section, use line breaks (\\n) to separate points. Make sure your feedback:
1. Directly connects to specific job requirements
2. Cites specific examples from candidate information
3. Is actionable and specific
4. Clearly explains any significant score impacts (especially low scores)
5. EXPLICITLY explains any score adjustments for commitment level or bio relevance

Do NOT include locationScore, availabilityScore, reputationScore, or finalScore as these will be calculated separately.
DO NOT include any text before or after the JSON object.
"""
    return prompt

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
        Dictionary containing evaluation scores and feedback for a single job application
    """
    # Configure the API
    if not configure_genai_api(api_key):
        return {"error": "Failed to configure API"}

    try:
        # Extract a single job application ID if we have multiple
        job_application_id = None
        if 'jobApplicationIds' in candidate_data and candidate_data['jobApplicationIds']:
            # Use the first application ID from the group
            job_application_id = candidate_data['jobApplicationIds'][0]
        elif 'jobApplicationId' in candidate_data:
            job_application_id = candidate_data['jobApplicationId']

        logger.info(f"Evaluating for job application ID: {job_application_id}")

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

            # Add the job application ID to the result
            if job_application_id is not None:
                result['jobApplicationId'] = job_application_id

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

                    # Add the job application ID to the result
                    if job_application_id is not None:
                        result['jobApplicationId'] = job_application_id

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