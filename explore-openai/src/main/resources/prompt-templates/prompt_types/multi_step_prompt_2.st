Perform the following actions on the text delimited by triple backticks:

    1. **Summarize** the text in 2 sentences.
    2. **Identify** the profession and hobbies of the person in the text.
    3. **Provide advice** for someone who shares the same profession on how to balance work and personal life.
    4. **Output** a JSON object with the following structure:

        {{
            "summary": "<your summary>",
            "profession": "<profession>",
            "hobbies": ["<hobby1>", "<hobby2>", ...],
            "advice": "<your advice>"
        }}

Please ensure your answers are separated by line breaks.

    Text to process:
    ```{input}```