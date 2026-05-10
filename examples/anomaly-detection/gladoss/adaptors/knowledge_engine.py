#!/usr/bin/env python

import logging
import os
import re
import tomllib
from typing import Any, Collection, Optional, Self

import requests
from rdf import BNode, IRIRef, Literal, Statement
from rdf.namespaces import RDF, RDFS, SHACL

from gladoss.adaptors.adaptor import Adaptor
from gladoss.core.connector import Connector

logger = logging.getLogger(__name__)


URI_CHARSET = r"[a-zA-Z0-9\-._~:/?#[\]@!$&'()*+,;=]"
URI = rf"<?{URI_CHARSET}+>?"
RESOURCE = rf"(?:{URI})|(?:\".*\"(?:(?:@[a-z]{{2}})|(?:\^\^{URI}))?)"
STATEMENT = re.compile(rf"(?P<head>{URI})"
                       rf"\s*(?P<relation>{URI})"
                       rf"\s*(?P<tail>{RESOURCE})\s*\.")
LITERAL = re.compile(r"(?P<value>\".*\")(?:"
                     r"(?:@(?P<lang>[a-z]{{2}}))|"
                     rf"(?:\^\^(?P<dtype>{URI})))?")

FILE_DIR = os.path.dirname(__file__)
FILENAME_CONF = "knowledge_engine.toml"
CONF_PATH = os.path.join(FILE_DIR, FILENAME_CONF)
REPORT_GRAPH_PATTERN = """
?report rdf:type sh:ValidationReport .
?report dct:date ?reportDate .
?report dct:identifier ?reportIdentifier .
?report dct:conformsTo ?reportLanguage .
?report sh:conforms ?validationPassed .
?report dct:hasPart ?result .

?result rdf:type sh:ValidationResult .
?result rdfs:label ?resultStatusMsg .
?result sh:focusNode ?resultFocusNode .
?result sh:resultPath ?resultPath .
?result sh:value ?resultValue .
?result sh:sourceShape ?resultSourceShape .
?result sh:resultMessage ?resultStatusMsgLong .
?result sh:resultSeverity ?resultSeverity .

?resultSeverity rdf:type sh:Severity .
?resultSeverity rdfs:label ?severityLabel .
?resultSeverity rdfs:comment ?severityDescription .
"""
REPORT_PREFIXES = {
        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
        "dct": "http://purl.org/dc/terms/",
        "sh": "http://www.w3.org/ns/shacl#"
        }
DCT = IRIRef("http://purl.org/dc/terms/")


class KE_Adaptor(Adaptor):
    """ Adaptor to TNO's Knowledge Engine

        Expects data in the form {"requestingKnowledgeBaseId": <INT>,
                                  "knowledgeInteractionId": <INT>,
                                  "handleRequestId": <INT>,
                                  "bindingSet": [{ "u": "<IRI>|<Literal>",
                                                   "v": "<IRI>|<Literal>",
                                                   ...},
                                                  ...]
                                 }
        with
        - "u", "v" the variable name, matching "?<var>"
        - { ... } a binding set between the variables and their values
        - [ ... ] a list of all possible binding sets for this observation
    """

    def register_kb(self: Self, endpoint: str, kb_id: str, kb_name: str,
                    kb_desc: str) -> bool:
        """ Register knowledge base if it has not been registered yet.

        :param endpoint: [TODO:description]
        :param kb_id: [TODO:description]
        :param kb_name: [TODO:description]
        :param kb_desc: [TODO:description]
        """
        endpoint = endpoint + "/sc"
        headers = {'Knowledge-Base-Id': kb_id,
                   'Content-Type': 'application/json'}

        response = requests.get(endpoint, headers=headers)
        if response.status_code == requests.codes.not_found:  # 404: not found
            payload = {'knowledgeBaseId': kb_id,
                       'knowledgeBaseName': kb_name,
                       'knowledgeBaseDescription': kb_desc}
            response = requests.post(endpoint, json=payload)

        return response.status_code == requests.codes.ok  # 200: ok

    def register_ki(self: Self, endpoint: str, kb_id: str,
                    ki_payload: dict[str, str]) -> Optional[str]:
        """ Register knowledge interaction.

        :param endpoint: [TODO:description]
        :param kb_id: [TODO:description]
        :param ki_payload: [TODO:description]
        """
        endpoint = endpoint + "/sc/ki"
        headers = {'Knowledge-Base-Id': kb_id,
                   'Content-Type': 'application/json'}

        response = requests.post(endpoint, headers=headers, json=ki_payload)
        if response.status_code == requests.codes.ok:  # 200: ok
            return response.json()['knowledgeInteractionId']  # type: str

    def deregister_kb(self: Self, endpoint: str, kb_id: str) -> bool:
        """ Deregister knowledge base.

        :param endpoint: [TODO:description]
        :param kb_id: [TODO:description]
        """
        endpoint = endpoint + "/sc"
        headers = {'Knowledge-Base-Id': kb_id}

        response = requests.delete(endpoint, headers=headers)

        return response.status_code == requests.codes.ok  # 200: ok

    def deregister_ki(self: Self, endpoint: str, kb_id: str, ki_id: str)\
            -> bool:
        """ Deregister knowledge interaction.

        :param endpoint: [TODO:description]
        :param kb_id: [TODO:description]
        :param ki_id: [TODO:description]
        """
        endpoint = endpoint + "/sc/ki"
        headers = {'Knowledge-Base-Id': kb_id,
                   'Knowledge-Interaction-Id': ki_id}

        response = requests.delete(endpoint, headers=headers)

        return response.status_code == requests.codes.ok  # 200: ok

    def post_ki(self: Self, endpoint: str, ki_headers: dict[str, Any],
                ki_payload: dict[str, Any]) -> bool:
        """ Perform a post knowledge interaction.

        :param endpoint: [TODO:description]
        :param ki_headers: [TODO:description]
        :param ki_payload: [TODO:description]
        """
        endpoint = endpoint + "/sc/post"
        response = requests.post(endpoint, headers=ki_headers, json=ki_payload)

        return response.status_code == requests.codes.ok  # 200: ok

    def init_hook(self: Self) -> None:
        """ Register the knowledge base and knowledge interactions. The
            knowledge interactions should be of the type 'react', and
            will have an empty results graph.
        """
        conf = dict()
        with open(CONF_PATH, 'rb') as f:
            conf = tomllib.load(f)

        # use context to share data between hooks
        self.context['reactKnowledgeInteractions'] = dict()
        self.context['reactKnowledgeInteractionsInv'] = dict()
        self.context['postKnowledgeInteractions'] = dict()
        self.context['postKowledgeInteractionConnectors'] = dict()
        self.context['argumentGraphPatterns'] = dict()

        kb_id = conf['knowledgeBaseId']
        kb_name = conf['knowledgeBaseName']
        kb_desc = conf['knowledgeBaseDescription']
        for ki in conf['knowledgeInteraction']:  # register all interactions
            ki_endpoint = ki['knowledgeInteractionEndpoint']
            if ki_endpoint not in self.context['reactKnowledgeInteractions']:
                # keep track of registered knowledge interactions
                self.context['reactKnowledgeInteractions'][ki_endpoint] = set()

            ki_pattern = ki['argumentGraphPattern']
            ki_prefixes = ki['prefixes']
            ki_payload = {
                    'knowledgeInteractionType': "ReactKnowledgeInteraction",
                    'knowledgeInteractionName': ki['knowledgeInteractionName'],
                    'argumentGraphPattern': ki_pattern,
                    'prefixes': ki['prefixes']
                    }
            try:
                if not self.register_kb(ki_endpoint, kb_id, kb_name, kb_desc):
                    raise Exception("Unable to register knowledge base")
                logger.info("Registered knowledge base at endpoint "
                            f"{ki_endpoint}")

                ki_id = self.register_ki(ki_endpoint, kb_id, ki_payload)
                if ki_id is None:
                    logger.error("Unable to register REACT knowledge "
                                 "interaction")

                    continue

                logger.info("Registered REACT knowledge interaction at "
                            f"endpoint {ki_endpoint}")

                self.context['reactKnowledge'
                             'Interactions'][ki_endpoint].add(ki_id)
                self.context['reactKnowledge'
                             'InteractionsInv'][ki_id] = ki_endpoint
                self.context['argumentGraphPatterns'][ki_id] = [ki_pattern,
                                                                ki_prefixes]
            except Exception:
                logger.error(f"Unable to register at endpoint '{ki_endpoint}'")

        self.context['knowledgeBaseId'] = kb_id

        # necessary for knowledge engine
        self.config.return_receipt = True

        # register KIs for report publication
        try:
            self.register_report_publications()
        except Exception:
            logger.error("Unable to register POST knowledge interaction")

    def register_report_publications(self: Self) -> None:
        """ Register a single post knowledge interaction per known
            endpoint to send validation reports to. The reports are
            send to the same endpoint as where the graph, about which
            the report reports, are received from.
        """
        kb_id = self.context['knowledgeBaseId']
        for ki_endpoint in self.context['reactKnowledgeInteractions'].keys():
            ki_payload = {
                    'knowledgeInteractionType': "PostKnowledgeInteraction",
                    'knowledgeInteractionName': "AnomalyReportPublication",
                    'argumentGraphPattern': REPORT_GRAPH_PATTERN,
                    'prefixes': REPORT_PREFIXES
                    }

            ki_id = self.register_ki(ki_endpoint, kb_id, ki_payload)
            if ki_id is None:
                logger.error("Unable to register post knowledge interaction")

                continue

            # keep track of registered knowledge interactions
            self.context['postKnowledgeInteractions'][ki_endpoint] = ki_id

            logger.info("Registered POST knowledge interaction at "
                        f"endpoint {ki_endpoint}")

    def cleanup_hook(self: Self):
        """ Deregister the knowledge base and all associated knowledge
            interactions.
        """
        kb_id = self.context['knowledgeBaseId']
        for ki_endpoint, ki_set in self.context['reactKnowledgeInteractions']:
            endpoint = ki_endpoint + "/sc/ki"
            for ki_id in ki_set:
                if not self.deregister_ki(endpoint, kb_id, ki_id):
                    logger.error("Unable to deregister knowledge interaction")

            if not self.deregister_kb(endpoint, kb_id):
                logger.error("Unable to deregister knowledge base")

    def publish_report(self: Self, identifier: str,
                       data: Collection[Statement]) -> bool:
        """ Publish the validation report (as N-Triples) for
            the state graph with the provided identifier, by
            performing a post knowledge interaction to the
            endpoint from which the state graph was received.

        :param identifier: the react knowledge interaction ID associated with
                           a distinct argument graph pattern.
        :param data: [TODO:description]
        :return: [TODO:description]
        """
        success = False
        try:
            package_headers = self.set_report_headers(identifier)
            package_payload = self.set_report_payload(identifier, data)

            ki_endpoint\
                = self.context['reactKnowledgeInteractionsInv'][identifier]

            logger.debug(f"POST {{\n{package_headers}\n{package_payload}\n}}")
            success = self.post_ki(ki_endpoint,
                                   package_headers,
                                   package_payload)
        except KeyError as e:
            logger.error(f"Unable to publish report to endpoint: {e}.")

        return success

    def add_connectors(self: Self) -> None:
        """ Add connectors to adaptor, one for each different endpoint.
        """
        # connections to listen on
        for ki_endpoint in self.context['reactKnowledgeInteractions'].keys():
            self.connectors.add(Connector(
                adaptor=self,
                endpoint=ki_endpoint + "/sc/handle",
                continuous=self.config.continuous,
                num_retries=self.config.retries,
                retry_delay=self.config.retry_delay,
                request_delay=self.config.request_delay,
                return_receipt=self.config.return_receipt
                ))

    def set_headers(self: Self) -> dict[str, Any]:
        """ Returns headers for polling the endpoint. Defaults
            to empty headers

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        kb_id = self.context['knowledgeBaseId']

        return {"Knowledge-Base-Id": kb_id}

    def set_payload(self: Self) -> list[Any] | dict[str, Any]:
        """ Returns payload for polling the endpoint. Defaults
            to empty payload.

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        return super().set_payload()

    def set_report_headers(self: Self, identifier: str) -> dict[str, Any]:
        """ Returns headers for publishing the validation report
            to the endpoint.

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        headers = dict()
        try:
            ki_endpoint\
                = self.context['reactKnowledgeInteractionsInv'][identifier]
            ki_id = self.context['postKnowledgeInteractions'][ki_endpoint]

            headers = {
                    'Knowledge-Base-Id': self.context['knowledgeBaseId'],
                    'Knowledge-Interaction-Id': ki_id,  # ID of post KI
                    'Content-Type': 'application/json'
                    }
        except KeyError:
            logger.error("Unable to retrieve identifiers for report "
                         "publication headers.")

        return headers

    def set_report_payload(self: Self, identifier: str,
                           data: Collection[Statement])\
            -> list[Any] | dict[str, Any]:
        """ Returns payload for publishing the validation report
            to the endpoint, by converting the provided shape graph
            to binding sets.

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        payload = self.translate_inv(data)

        return payload

    def set_receipt_headers(self: Self, data: dict[str, Any])\
            -> dict[str, Any]:
        """ Returns headers for sending a receipt.

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        headers = dict()
        try:
            headers = {
                    'Knowledge-Base-Id': self.context['knowledgeBaseId'],
                    'Knowledge-Interaction-Id': data['knowledgeInteractionId'],
                    'Content-Type': 'application/json'
                    }
        except KeyError:
            logger.error("Unable to retrieve identifiers from message "
                         + "payload.")

        return headers

    def set_receipt_payload(self: Self, data: dict[str, Any])\
            -> list[Any] | dict[str, Any]:
        """ Returns payload for sending a receipt.

        :param self: [TODO:description]
        :return: [TODO:description]
        """
        payload = dict()
        try:
            req_id = data['handleRequestId']
            payload = {
                    'handleRequestId': req_id,
                    'bindingSet': []  # empty response
                    }
        except KeyError:
            logger.error("Unable to retrieve identifiers from message "
                         + "payload.")

        return payload

    def translate(self: Self, data: dict[str, Any])\
            -> list[tuple[str, list[Statement]]]:
        """ Translate binding sets to RDF.

        :param data: data received from API
        :return: A list of RDF statements and anchors
        :raises SyntaxWarning: warn if translation fails
        """

        data_translated = list()
        if "bindingSet" not in data.keys() or len(data["bindingSet"]) <= 0:
            logging.debug("Missing content in data package")
            return data_translated

        if "knowledgeInteractionId" not in data.keys():
            logging.debug("Missing graph identifier in data package")
            return data_translated

        ki_id = data['knowledgeInteractionId']  # type: str
        if ki_id not in self.context['argumentGraphPatterns'].keys():
            logging.debug("Unable to find argument graph pattern associated "
                          + f"with knowledge interaction {ki_id}")
            return data_translated

        ki_pattern, ki_prefixes = self.context['argumentGraphPatterns'][ki_id]
        bindings = data["bindingSet"]  # type: list[dict[str,str]]
        try:
            statements_lst = self.create_pattern_template(ki_pattern,
                                                          ki_prefixes)
            for bset in bindings:
                graph = list()
                for s in self.instantiate_graph(statements_lst, bset):
                    match = re.fullmatch(STATEMENT, s)
                    if match is not None:
                        fact = self.process_fact(match)

                        graph.append(fact)

                data_translated.append((ki_id, graph))
        except Exception:
            raise SyntaxWarning(f"Unexpected data format: {bindings}")

        return data_translated

    def translate_inv(self: Self, data: Collection[Statement])\
            -> list[dict[str, str]]:
        """ Translates a validation report into a binding set with as many
            entries as there are results. Returns an empty binding set if
            no results are to be reported.

        :param data:
        :return: [TODO:description]
        """
        def to_string(node: IRIRef | Literal) -> str:
            if isinstance(node, Literal):
                node_str = f"\"{node}\""
                if node.datatype is not None:
                    node_str += f"^^{to_string(node.datatype)}"
                elif node.language is not None:
                    node_str += f"@{node.language}"

                return node_str
            elif isinstance(node, BNode):
                return f"_:{node.value}"

            return f"<{node}>"

        binding_set = list()

        root = None
        report = dict()
        results = dict()
        severities = dict()
        for statement in data:
            if statement.predicate == RDF + "type":
                sbj = statement.subject
                if statement.object == SHACL + "ValidationReport":
                    root = sbj
                    report[sbj] = {"report": sbj}

                    continue
                if statement.object == SHACL + "ValidationResult":
                    results[sbj] = {"result": sbj}

                    continue

                if statement.object == SHACL + "Severity":
                    severities[sbj] = dict()

                    continue

        if root is not None:
            for statement in data:
                sbj = statement.subject
                if sbj == root:
                    if statement.predicate == DCT + "date":
                        report[sbj]["reportDate"] = statement.object

                        continue
                    if statement.predicate == DCT + "identifier":
                        report[sbj]["reportIdentifier"] = statement.object

                        continue
                    if statement.predicate == DCT + "conformsTo":
                        report[sbj]["reportLanguage"] = statement.object

                        continue
                    if statement.predicate == SHACL + "conforms":
                        report[sbj]["validationPassed"] = statement.object

                        continue

                if sbj in results.keys():
                    if statement.predicate == RDFS + "label":
                        results[sbj]["resultStatusMsg"] = statement.object

                        continue
                    if statement.predicate == SHACL + "focusNode":
                        results[sbj]["resultFocusNode"] = statement.object

                        continue
                    if statement.predicate == SHACL + "resultPath":
                        results[sbj]["resultPath"] = statement.object

                        continue
                    if statement.predicate == SHACL + "value":
                        results[sbj]["resultValue"] = statement.object

                        continue
                    if statement.predicate == SHACL + "sourceShape":
                        results[sbj]["resultSourceShape"] = statement.object

                        continue
                    if statement.predicate == SHACL + "resultMessage":
                        results[sbj]["resultStatusMsgLong"] = statement.object

                        continue
                    if statement.predicate == SHACL + "resultSeverity":
                        results[sbj]["resultSeverity"] = statement.object

                        continue

                if sbj in severities.keys():
                    if statement.predicate == RDFS + "label":
                        severities[sbj]["severityLabel"] = statement.object

                        continue
                    if statement.predicate == RDFS + "comment":
                        severities[sbj]["severityDescription"]\
                                = statement.object

                        continue

            # integrate dictionaries
            for result in results.values():
                bindings = dict()
                for k, v in report[root].items():
                    bindings[k] = to_string(v)

                for k, v in result.items():
                    bindings[k] = to_string(v)

                if "resultSeverity" in result.keys():
                    sev = result["resultSeverity"]
                    if sev in severities.keys():
                        for k, v in severities[sev].items():
                            bindings[k] = to_string(v)

                binding_set.append(bindings)

        return binding_set

    def create_pattern_template(self, pattern: str, prefixes: dict[str, str])\
            -> list[str]:
        """ Expand prefixes by their full namespaces.

        :param prefixes: [TODO:description]
        :return: [TODO:description]
        """
        out = list()
        for s in pattern.splitlines():
            s = s.strip()
            if len(s) <= 0:
                continue
            if ':' not in s:
                out.append(s)

                continue

            i = 0
            in_prefix = False
            in_iri = False
            s_lst = list()
            for j in range(len(s)):
                char = s[j]
                if char == '"':
                    # reached a literal; no need to continue since
                    # the pattern does not support datatype annotations
                    j = len(s)

                    break

                if char == "<":
                    in_iri = True
                if char == ">" and in_iri:
                    in_iri = False
                if in_iri:
                    continue

                if char == ':' and j > 0:
                    # prefix deliminator
                    h = i  # remember original end
                    i = j - 1
                    while i > 0:
                        if s[i].isspace():  # pre prefix start
                            i += 1

                            break

                        i -= 1

                    prefix = s[i:j]
                    ns = prefixes.get(prefix)
                    if ns is not None:
                        s_lst.append(s[h:i])  # add prior
                        s_lst.append("<" + ns)

                        i = j + 1
                        in_prefix = True
                    else:
                        i = h  # reset to original

                if in_prefix and char.isspace():  # end of prefixed IRI
                    s_lst.append(s[i:j] + ">")

                    in_prefix = False
                    i = j

            s_lst.append(s[i:j+1])  # add remainder
            out.append(''.join(s_lst))

        return out

    def instantiate_graph(self: Self, statements_str: list[str],
                          bindings: dict[str, str]) -> list[str]:
        """ Replace variable names by bounded values.

        :param statements_str: the triple pattern as strings
        :param bindings: a map between variable names and their values
        :return: the instantiated triples as strings
        """

        out = list()
        for s in statements_str:
            if len(s) <= 0:
                continue

            if '?' not in s:
                out.append(s)

                continue

            i = 0
            flag = False
            s_lst = list()
            for j in range(len(s)):
                char = s[j]
                if char == '"':
                    # reached a literal; no need to continue
                    j = len(s)

                    break

                if char == '?':
                    # possible variable
                    if i > 0:
                        # add chars since last variable
                        s_lst.append(s[i:j])

                    i = j
                    flag = True

                    continue

                if flag and char.isspace():  # end of variable
                    var = s[i+1:j]
                    if var in bindings.keys():
                        if len(s_lst) <= 0 and i > 0:
                            # add chars in front
                            s_lst.append(s[:i])

                        binding = bindings[var]
                        s_lst.append(binding)

                    i = j
                    flag = False

                    continue

            s_lst.append(s[i:j+1])  # add remainder
            out.append(''.join(s_lst))

        return out

    def process_fact(self: Self, match: re.Match) -> Statement:
        """ Process single string-encoded fact and return a RDF statement.

        :param match: A matching regex object
        :return: The corresponding RDF statement
        """
        head = IRIRef(match.group('head')[1:-1])
        relation = IRIRef(match.group('relation')[1:-1])

        tail = match.group('tail')  # type: str
        tail_literal = re.fullmatch(LITERAL, tail)
        if tail_literal:
            value = tail_literal.group('value')
            lang = tail_literal.group('lang')
            dtype = tail_literal.group('dtype')

            if dtype:
                dtype = IRIRef(dtype[1:-1])

            value = value[1: -1]  # strip escaped quotation marks
            tail = Literal(value, datatype=dtype, language=lang)
        else:
            tail = IRIRef(tail[1:-1])

        return Statement(head, relation, tail)
