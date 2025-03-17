Perform the following actions on the text delimited by triple backticks:

    1. Summarize the text in one sentence.
    2. Translate the summary into French.
    3. List all the key characters mentioned in the text.
    4. Output a JSON object with the structure:
    {{
        "summary": "<your summary>",
        "translation": "<translated summary>",
        "people": ["<person1>", "<person2>", ...]
    }}

    ```{input}```