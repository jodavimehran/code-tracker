import csv

# 1. Copy the summary-*-train and summary-*-test files here
# 2. Change the tool variable below (Pascal Case)
# The txt files will be generated in the same directory
tool = "Tracker"

# Define the input CSV file names
input_csv_file1 = f"summary-{tool.lower()}-training.csv"
input_csv_file2 = f"summary-{tool.lower()}-test.csv"

# Define the output TXT file names
output_txt_file1 = f"summary-{tool.lower()}-training.txt"
output_txt_file2 = f"summary-{tool.lower()}-test.txt"
output_txt_file3 = f"summary-{tool.lower()}-all.txt"

# Function to extract processing times from a CSV file and write to a TXT file
def process_csv_to_txt(input_csv_file, output_txt_file, tool_name):
    with open(input_csv_file, 'r') as csv_file, open(output_txt_file, 'w') as txt_file:
        csv_reader = csv.DictReader(csv_file)
        # header = next(csv_reader)  # Skip the header row
        txt_file.write("Tool, Runtime\n")
        for row in csv_reader:
            processing_time = row['processing_time']
            txt_file.write(f"{tool_name}, {processing_time}\n")

# Process the first CSV file and write to the first TXT file
process_csv_to_txt(input_csv_file1, output_txt_file1, f"{tool}_train")

# Process the second CSV file and write to the second TXT file
process_csv_to_txt(input_csv_file2, output_txt_file2, f"{tool}_test")

# Create the third TXT file by merging data from the first two files
with open(output_txt_file1, 'r') as file1, open(output_txt_file2, 'r') as file2, open(output_txt_file3, 'w') as file3:
    file3.write("Tool, Runtime\n")
    for line in file1:
        if line.strip() != "Tool, Runtime":
            file3.write(f"{tool}_all, {line.split(', ')[1]}")
    for line in file2:
        if line.strip() != "Tool, Runtime":
            file3.write(f"{tool}_all, {line.split(', ')[1]}")

print("Conversion completed.")