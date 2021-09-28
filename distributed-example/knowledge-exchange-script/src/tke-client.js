export class TkeClient {
  constructor(smartConnectorRestEndpoint) {
    this.smartConnectorRestEndpoint = smartConnectorRestEndpoint;
  }

  async registerKnowledgeBase(id, name, description, reregister, lease) {
    let getResponse = await fetch(
      this.smartConnectorRestEndpoint + '/sc',
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Knowledge-Base-Id': id
        },
      }
    );

    if (getResponse.ok) {
      if (reregister != undefined && reregister === true) {
        let existingKb = new KnowledgeBase(this.smartConnectorRestEndpoint, id);
        await existingKb.unregister();
      } else {
        throw new Error(`Knowledge base with ID ${id} already exists.`);
      }
    }

    const kbRegistrationBody = {
      knowledgeBaseId: id,
      knowledgeBaseName: name,
      knowledgeBaseDescription: description
    }

    if (lease != undefined) {
      kbRegistrationBody.leaseRenewalTime = lease;
    }

    let response = await fetch(
      this.smartConnectorRestEndpoint + '/sc',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(kbRegistrationBody)
      }
    );
    if (!response.ok) {
      throw new Error(await response.text());
    } else {
      return new KnowledgeBase(this.smartConnectorRestEndpoint, id, lease)
    }
  }

  async getKnowledgeBases(knowledgeBaseId) {
    let headers = {
      'Content-Type': 'application/json',
    }

    if (knowledgeBaseId != undefined) {
      headers['Knowledge-Base-Id'] = knowledgeBaseId
    }

    let response = await fetch(
      this.smartConnectorRestEndpoint + '/sc',
      {
        method: 'GET',
        headers: {
          'Knowledge-Base-Id': knowledgeBaseId
        },
      }
    );
    if (!response.ok) {
      throw new Error(await response.text());
    } else {
      return await response.json();
    }
  }
}

export class KnowledgeBase {
  constructor(smartConnectorRestEndpoint, knowledgeBaseId, lease) {
    this.smartConnectorRestEndpoint = smartConnectorRestEndpoint;
    this.knowledgeBaseId = knowledgeBaseId;
    this.handlers = new Map();
    this.longPolling = false;
    this.lease = lease;
    if (this.lease != undefined) {
      this.scheduleRenewLease();
    }
  }

  async unregister() {
    let response = await fetch(
      this.smartConnectorRestEndpoint + '/sc',
      {
        method: 'DELETE',
        headers: {
          'Knowledge-Base-Id': this.knowledgeBaseId,
        }
      }
    );
    if (!response.ok) {
      throw new Error(await response.text());
    }
  }

  async registerKnowledgeInteraction(type, graphPattern, argumentGraphPattern, resultGraphPattern, handler) {
    let body = {
      knowledgeInteractionType: type,
    };
    if (graphPattern) {
      body.graphPattern = graphPattern;
    }
    if (argumentGraphPattern) {
      body.argumentGraphPattern = argumentGraphPattern;
    }
    if (resultGraphPattern) {
      body.resultGraphPattern = resultGraphPattern;
    }

    let response = await fetch(
      this.smartConnectorRestEndpoint + '/sc/ki',
      {
        method: 'POST',
        headers: {
          'Knowledge-Base-Id': this.knowledgeBaseId,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body)
      }
    );

    if (!response.ok) {
      throw new Error(await response.text());
    }
    let knowledgeInteractionId = await response.text();

    if (handler != undefined && (type == 'AnswerKnowledgeInteraction' || type == 'ReactKnowledgeInteraction')) {
      // For reactive KI's, we store the handler that the caller provided. These
      // will be called (with the incoming bindings) when the KI is triggered.
      this.handlers.set(knowledgeInteractionId, handler);
      if (!this.longPolling) {
        // We call this async function, but don't await it as it will loop indefinitely!
        this.startLongPoll();
      }
    } else if (type == 'AskKnowledgeInteraction' || type == 'PostKnowledgeInteraction') {
      // For proactive KI's, we return a function that can be called when the KI
      // has to be triggered.
      return async (bindingSet) => {
        let path;
        if (type == 'AskKnowledgeInteraction') {
          path = '/sc/ask';
        } else if ('PostKnowledgeInteraction') {
          path = '/sc/post';
        }
        let response = await fetch(
          this.smartConnectorRestEndpoint + path,
          {
            method: 'POST',
            headers: {
              'Knowledge-Base-Id': this.knowledgeBaseId,
              'Knowledge-Interaction-Id': knowledgeInteractionId,
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(bindingSet),
          }
        );

        if (!response.ok) {
          throw new Error(await response.text());
        }

        return await response.json();
      }
    }
  }

  scheduleRenewLease() {
    setTimeout(
      async () => {
        let scLease = await this.renewLease();
        console.log(`renewed lease for ${scLease.knowledgeBaseId} until ${new Date(scLease.expires)}`);
        // Immediately schedule a new renewal recursively.
        this.scheduleRenewLease();
      },
      (this.lease * 1000) * 0.8 // Renew lease when it is 80% passed
    );
  }

  async renewLease() {
    let response = await fetch(
      `${this.smartConnectorRestEndpoint}/sc/lease/renew`,
      {
        method: 'PUT',
        headers: {
          'Knowledge-Base-Id': this.knowledgeBaseId,
        }
      }
    );
    if (!response.ok) {
      throw new Error(await response.text());
    } else {
      return await response.json();
    }
  }

  async registerAskKnowledgeInteraction(graphPattern) {
    return await this.registerKnowledgeInteraction('AskKnowledgeInteraction', graphPattern);
  }

  async registerAnswerKnowledgeInteraction(graphPattern, handler) {
    return await this.registerKnowledgeInteraction('AnswerKnowledgeInteraction', graphPattern, undefined, undefined, handler);
  }

  async registerPostKnowledgeInteraction(argumentGraphPattern, resultGraphPattern) {
    return await this.registerKnowledgeInteraction('PostKnowledgeInteraction', undefined, argumentGraphPattern, resultGraphPattern, undefined);
  }

  async registerReactKnowledgeInteraction(argumentGraphPattern, resultGraphPattern, handler) {
    return await this.registerKnowledgeInteraction('ReactKnowledgeInteraction', undefined, argumentGraphPattern, resultGraphPattern, handler);
  }

  async startLongPoll() {
    this.longPolling = true;
    while (true) {
      console.log('awaiting long poll');
      let getResponse = await fetch(
        this.smartConnectorRestEndpoint + '/sc/handle',
        {
          method: 'GET',
          headers: {
            'Knowledge-Base-Id': this.knowledgeBaseId,
          },
        }
      );

      if (!getResponse.ok) {
        throw new Error(await getResponse.text());
      } else if (getResponse.status == 202) {
        continue;
      } else {
        let handleRequest = await getResponse.json();

        let handleRequestId = handleRequest.handleRequestId;
        let handler = this.handlers.get(handleRequest.knowledgeInteractionId)
        let incomingBindings = handleRequest.bindingSet

        let outgoingBindings = handler(incomingBindings);
        if (outgoingBindings == undefined) {
          console.warn('Using empty binding set as ANSWER/REACT, since the handler returned nothing.')
          outgoingBindings = [];
        }

        let postResponse = await fetch(
          this.smartConnectorRestEndpoint + '/sc/handle',
          {
            method: 'POST',
            headers: {
              'Knowledge-Base-Id': this.knowledgeBaseId,
              'Knowledge-Interaction-Id': handleRequest.knowledgeInteractionId,
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              handleRequestId: handleRequestId,
              bindingSet: outgoingBindings,
            }),
          }
        );
        if (!postResponse.ok) {
          throw new Error(await postResponse.text());
        }
      }
    }
  }
}
