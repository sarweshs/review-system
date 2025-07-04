# review-python

This directory is for Python-based microservices or utilities related to the review system monorepo.

## Setup

1. Create a virtual environment:
   ```sh
   python3 -m venv venv
   source venv/bin/activate
   ```
2. Install dependencies:
   ```sh
   pip install -r requirements.txt
   ```

## Add your Python code here 

## When initializing the vault you may get error as vault is sealed follow the steps below to unseal it
docker exec -it vault vault operator init -address=http://127.0.0.1:8200
It will give you 3 unseal keys and a root token, copy them to a safe place. or put it in zshrc file
```sh
Then run the following command to unseal the vault
```sh
