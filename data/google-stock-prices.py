import kagglehub

# Download latest version
path = kagglehub.dataset_download("youssefelebiary/google-stock-prices-2015-2024")

print("Path to dataset files:", path)