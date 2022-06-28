import re

modes = [
    'docker',
    'docker-compose',
    'dockerfile'
]


def check_ports(port: str):
    return re.match(r'^\d+:\d+$', port)
