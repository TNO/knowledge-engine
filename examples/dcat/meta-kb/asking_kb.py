import os
import logging
import time
import json
import signal
import requests

from knowledge_mapper.tke_client import TkeClient
from knowledge_mapper.knowledge_base import KnowledgeBaseRegistrationRequest
from knowledge_mapper.knowledge_interaction import (
    AskKnowledgeInteraction,
    AskKnowledgeInteractionRegistrationRequest,
)

KE_URL = os.getenv("KE_URL")
KB_ID = os.getenv("KB_ID")
KB_NAME = KB_ID.split("/")[-1]
if "PREFIXES" in os.environ:
    PREFIXES = json.loads(os.getenv("PREFIXES"))
else:
    PREFIXES = None
GRAPH_PATTERN = os.getenv("GRAPH_PATTERN")
DOMAIN_KNOWLEDGE= os.getenv("DOMAIN_KNOWLEDGE")

log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)

"""
Make sure that this Python program handles SIGTERM by raising a
KeyboardInterrupt, to end the program.
"""
def handle_sigterm(*args):
    raise KeyboardInterrupt()

signal.signal(signal.SIGTERM, handle_sigterm)

def register_domain_knowledge(domain_knowledge):
    resp = requests.post(url = KE_URL + "/sc/knowledge", headers = {"Knowledge-Base-Id":KB_ID}, data = DOMAIN_KNOWLEDGE);
    if resp.status_code != 200:
        log.error(f"Our domain knowledge register should return 200 and not {resp.status_code} with message: " + resp.text);
        
def register_ask_knowledge_interaction(graph_pattern, prefixes) -> dict:
    resp = requests.post(url = KE_URL + "/sc/ki", headers = {"Knowledge-Base-Id":KB_ID}, json = { "knowledgeInteractionType": "AskKnowledgeInteraction", "graphPattern": graph_pattern, "prefixes": prefixes, "includeMetaKIs": "true"});
    if resp.status_code != 200:
        log.error(f"Our ask KI register should return 200 and not {resp.status_code} with message: " + resp.text);
    else:
        log.info(f"Registering Ask succesfull!")
    return resp.json();

def ask(kiId, bindingSet: dict ) -> dict:
    resp = requests.post(
        KE_URL + '/sc/ask',
        json=bindingSet,
        headers={
            'Knowledge-Base-Id': KB_ID,
            'Knowledge-Interaction-Id': kiId,
        }
    )

    if resp.status_code != 200:
        log.error(f"Our ask should return 200 and not {resp.status_code} with message: " + resp.text);
    else:
        log.info(f"Ask activated succesfully!")
    
    return resp.json()

def kb_1():
    client = TkeClient(KE_URL)
    client.connect()
    log.info(f"registering KB...")
    kb = client.register(
        KnowledgeBaseRegistrationRequest(
            id=f"{KB_ID}",
            name=f"{KB_NAME}",
            description=f"{KB_NAME}",
        )
    )
    
    # register domain knowledge
    if DOMAIN_KNOWLEDGE is not None:
        register_domain_knowledge(DOMAIN_KNOWLEDGE)
        log.info("Domain knowledge registered!")
    else:
        log.debug(f"No domain knowledge found!")
    
    log.info(f"KB registered!")
    log.info(f"registering ASK KI...")
    
    kiId = register_ask_knowledge_interaction(GRAPH_PATTERN, PREFIXES)["knowledgeInteractionId"]
    
    log.info(f"ASK KI ({kiId}) registered!")
    result = []
    try:
	    while True:
	        log.info(f"asking...")
	        result = ask(kiId, [{}])["bindingSet"]
	        if len(result) == 0:
	            log.info(f"asking gave no results; will sleep for 2s...")
	        else:
	            log.info(f"got answer: {result}")
	        time.sleep(2)
    finally:
        log.info(f"unregistering...")
        kb.unregister()


if __name__ == "__main__":
    kb_1()
