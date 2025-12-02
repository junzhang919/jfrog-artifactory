import requests
import os
import mysql.connector

ARTIFACTORY_URL = "https://arti.private-domain/artifactory"
primary_b64auth = "YWRtaW46MDVkMDNkMGNmMGY1ZGQwNDFkMzgzMTMxNzliMmYzOGE="

REPOS = ["docker-local", "generic-local"]
AUTH_HEADER = {
    'Content-Type': 'application/json',
    'Authorization': 'Basic {}'.format(primary_b64auth)
}

def list_folders(repo_path):
    url = f"{ARTIFACTORY_URL}/api/storage/{repo_path}"
    response = requests.get(url, headers=AUTH_HEADER)

    if response.status_code == 200:
        data = response.json()
        folders = [child['uri'].strip('/') for child in data.get('children', []) if child.get('folder', False)]
        return folders
    else:
        print(f"Error: {response.status_code} {response.text}")
        return []


def get_folder_info(repo_name, node_path):
    config = {
        'host': 'artifactory-db-ha.us-east-1.rds.amazonaws.com',
        'user': 'artifactory',
        'password': '920e3006a969c53144d2b3fbafe5a646',
        'database': 'artifactory'
    }

    query = """
    SELECT COUNT(*) AS file_count, SUM(bin_length) AS total_size
    FROM nodes
    WHERE node_type = 1
      AND repo = %s
      AND depth > %s
      AND (node_path = %s OR node_path LIKE %s)
    """
    current_depth = 1
    like_path = f'{node_path}/%'

    try:
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        cursor.execute(query, (repo_name, current_depth, node_path, like_path))

        result_row = cursor.fetchone()
        if result_row is None or len(result_row) != 2:
            return 0, 0

        file_count, total_size = result_row
        file_count = file_count if file_count is not None else 0
        total_size = total_size if total_size is not None else 0

        return file_count, total_size

    except mysql.connector.Error as err:
        return 0, 0
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

# --- Main ---
if __name__ == "__main__":
    with open("/opt/node_exporter/artifactory/artifactory_folder_storage_info.temp", "w") as tmp_file:
        tmp_name = tmp_file.name
        for repo in REPOS:
            folders = list_folders(repo)
            for folder in folders:
                if folder == ".jfrog":
                    continue
                folder_path = f"{repo}/{folder}"
                artifacts_count, size_in_bytes = get_folder_info(repo, folder)
                print(f'arti_repo{{key="folder_artifacts_count",folder_key="{folder_path}"}} {float(artifacts_count):.6f}', file=tmp_file)
                print(f'arti_repo{{key="folder_artifacts_size",folder_key="{folder_path}"}} {float(size_in_bytes):.6f}', file=tmp_file)

    os.replace(tmp_name, "/opt/node_exporter/artifactory_folder_storage_metric.prom")