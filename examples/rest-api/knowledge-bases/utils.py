import logging
import time

import requests

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def add_sigterm_hook():
    """
    Make sure that this Python program handles SIGTERM by raising a
    KeyboardInterrupt, to end the program.
    """
    import signal

    def handle_sigterm(*args):
        raise KeyboardInterrupt()

    signal.signal(signal.SIGTERM, handle_sigterm)


def match_bindings(
    query: list[dict[str, str]], source: list[dict[str, str]]
) -> list[dict[str, str]]:
    """
    Given a list of query bindings and a list of source bindings, return the
    list of source bindings that match to the query.

    >>> match_bindings(
    ...     [{"a": "<http://example.org/1>"}],
    ...     [{"a": "<http://example.org/1>"}, {"a": "<http://example.org/2>"}]
    ... )
    [{'a': '<http://example.org/1>'}]
    """
    matches = []
    for s in source:
        for q in query:
            q_matches = True
            for k, v in q.items():
                if not (k in s and s[k] == v):
                    q_matches = False
                    break
            if q_matches:
                matches.append(s.copy())
                break
    return matches


def register_knowledge_base(
    kb_id: str, kb_name: str, kb_description: str, ke_endpoint: str
):
    """
    Register a Knowledge Base with the given details at the given endpoint.
    """
    response = requests.post(
        ke_endpoint + "sc/",
        json={
            "knowledgeBaseId": kb_id,
            "knowledgeBaseName": kb_name,
            "knowledgeBaseDescription": kb_description,
        },
    )
    assert response.ok

    logger.info(f"registered {kb_name}")


def register_ask_knowledge_interaction(
    graph_pattern: str,
    ki_name: str,
    kb_id: str,
    ke_endpoint: str,
    prefixes: dict[str, str] = dict(),
) -> str:
    body = {
        "knowledgeInteractionName": ki_name,
        "knowledgeInteractionType": "AskKnowledgeInteraction",
        "graphPattern": graph_pattern,
        "prefixes": prefixes,
    }

    response = requests.post(
        ke_endpoint + "sc/ki/",
        json=body,
        headers={"Knowledge-Base-Id": kb_id},
    )
    assert response.ok

    ki_id = response.json()["knowledgeInteractionId"]
    logger.info(f"received issued knowledge interaction id: {ki_id}")
    return ki_id


def register_answer_knowledge_interaction(
    graph_pattern: str,
    ki_name: str,
    kb_id: str,
    ke_endpoint: str,
    prefixes: dict[str, str] = dict(),
) -> str:
    body = {
        "knowledgeInteractionName": ki_name,
        "knowledgeInteractionType": "AnswerKnowledgeInteraction",
        "graphPattern": graph_pattern,
        "prefixes": prefixes,
    }

    response = requests.post(
        ke_endpoint + "sc/ki/",
        json=body,
        headers={"Knowledge-Base-Id": kb_id},
    )
    assert response.ok

    ki_id = response.json()["knowledgeInteractionId"]
    logger.info(f"received issued knowledge interaction id: {ki_id}")
    return ki_id


def register_post_knowledge_interaction(
    argument_graph_pattern: str,
    result_graph_pattern: str,
    ki_name: str,
    kb_id: str,
    ke_endpoint: str,
    prefixes: dict[str, str] = dict(),
) -> str:
    body = {
        "knowledgeInteractionName": ki_name,
        "knowledgeInteractionType": "PostKnowledgeInteraction",
        "argumentGraphPattern": argument_graph_pattern,
        "prefixes": prefixes,
    }

    if result_graph_pattern is not None:
        body["argumentGraphPattern"] = result_graph_pattern

    response = requests.post(
        ke_endpoint + "sc/ki/",
        json=body,
        headers={"Knowledge-Base-Id": kb_id},
    )
    assert response.ok

    ki_id = response.json()["knowledgeInteractionId"]
    logger.info(f"received issued knowledge interaction id: {ki_id}")
    return ki_id


def register_react_knowledge_interaction(
    argument_graph_pattern: str,
    result_graph_pattern: str,
    ki_name: str,
    kb_id: str,
    ke_endpoint: str,
    prefixes: dict[str, str] = dict(),
) -> str:
    body = {
        "knowledgeInteractionName": ki_name,
        "knowledgeInteractionType": "ReactKnowledgeInteraction",
        "argumentGraphPattern": argument_graph_pattern,
        "prefixes": prefixes,
    }

    if result_graph_pattern is not None:
        body["argumentGraphPattern"] = result_graph_pattern

    response = requests.post(
        ke_endpoint + "sc/ki/",
        json=body,
        headers={"Knowledge-Base-Id": kb_id},
    )
    assert response.ok

    ki_id = response.json()["knowledgeInteractionId"]
    logger.info(f"received issued knowledge interaction id: {ki_id}")
    return ki_id


def ask(
    query_bindings: list[dict[str, str]], ki_id: str, kb_id: str, ke_endpoint: str
) -> list[dict[str, str]]:
    """
    ASK for knowledge with query bindings to receive bindings for an ASK knowledge interaction.
    """
    response = requests.post(
        ke_endpoint + "sc/ask",
        headers={"Knowledge-Base-Id": kb_id, "Knowledge-Interaction-Id": ki_id},
        json=query_bindings,
    )
    assert response.ok

    return response.json()["bindingSet"]


def post(
    bindings: list[dict[str, str]], ki_id: str, kb_id: str, ke_endpoint: str
) -> list[dict[str, str]]:
    """
    POST bindings for a POST knowledge interactions
    """
    response = requests.post(
        ke_endpoint + "sc/post",
        headers={"Knowledge-Base-Id": kb_id, "Knowledge-Interaction-Id": ki_id},
        json=bindings,
    )
    assert response.ok

    return response.json()["resultBindingSet"]


def start_handle_loop(handlers: dict[str, callable], kb_id: str, ke_endpoint: str):
    """
    Start the handle loop, where it will long poll to a route that returns a
    handle request when it arrives.

    Once a handle request is returns (on status 200) for an ANSWER/REACT, it
    will be handled by the corresponding knowledge interaction handler given in
    `handlers` (keyed by the knowledge interaction ID), and the result is passed
    back to the KE.
    """
    while True:
        response = requests.get(
            ke_endpoint + "sc/handle", headers={"Knowledge-Base-Id": kb_id}
        )

        if response.status_code == 200:
            # 200 means: we receive bindings that we need to handle, then repoll asap.
            handle_request = response.json()

            ki_id = handle_request["knowledgeInteractionId"]
            handle_request_id = handle_request["handleRequestId"]
            bindings = handle_request["bindingSet"]

            assert ki_id in handlers
            handler = handlers[ki_id]

            # pass the bindings to the handler, and let it handle them
            result_bindings = handler(bindings)

            handle_response = requests.post(
                ke_endpoint + "sc/handle",
                json={
                    "handleRequestId": handle_request_id,
                    "bindingSet": result_bindings,
                },
                headers={
                    "Knowledge-Base-Id": kb_id,
                    "Knowledge-Interaction-Id": ki_id,
                },
            )
            assert handle_response.ok

            continue
        elif response.status_code == 202:
            # 202 means: repoll (heartbeat)
            continue
        elif response.status_code == 410:
            # 410 means: KE has stopped, so terminate
            break
        else:
            logger.warn(f"received unexpected status {response.status_code}")
            logger.warn(response.text)
            logger.info("repolling after a short timeout")
            time.sleep(2)
            continue

    logger.info(f"exiting handle loop")
