
import requests
import base64
import json

url = "http://localhost:8080/api/auth/login"
payload = {"username": "moderator@examportal.com", "password": "password123"}
headers = {"Content-Type": "application/json"}

try:
    response = requests.post(url, json=payload)
    print(f"Status Code: {response.status_code}")
    print(f"Response Body: {response.text}")

    if response.status_code == 200:
        data = response.json()
        token = data.get("token")
        print(f"\nToken: {token}")
        
        # Decode JWT (no validation, just inspecting payload)
        parts = token.split(".")
        if len(parts) == 3:
            payload_padded = parts[1] + "=" * (-len(parts[1]) % 4)
            decoded_bytes = base64.urlsafe_b64decode(payload_padded)
            decoded_str = decoded_bytes.decode("utf-8")
            print(f"\nDecoded Payload: {decoded_str}")
        else:
            print("\nInvalid JWT format")
    else:
        print("\nLogin Failed")

except Exception as e:
    print(f"Error: {e}")
