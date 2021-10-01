import { TkeClient, KnowledgeBase } from './tke-client.js';
import { sleep } from './utils.ts';

run();

async function run() {
  const client1 = new TkeClient('http://runtime-1:8280/rest');
  const client2 = new TkeClient('http://runtime-2:8280/rest');
  console.info('sleeping for 5 seconds to let runtimes boot');
  await sleep(5000);

  let kb1: KnowledgeBase | undefined;
  while (kb1 == undefined) {
    try {
      console.info('connecting to runtime 1');
      kb1 = await client1.registerKnowledgeBase('http://example.org/kb1', 'KB1', 'An example KB1');
    } catch (e) {
      console.warn(e);
      console.info('retrying in 2 seconds');
      await sleep(2000);
    }
  }
  console.info('connected to runtime 1');

  let kb2: KnowledgeBase | undefined;
  while (kb2 == undefined) {
    try {
      console.info('connecting to runtime 2');
      kb2 = await client2.registerKnowledgeBase('http://example.org/kb2', 'KB2', 'An example KB2');
    } catch (e) {
      console.warn(e);
      console.info('retrying in 2 seconds');
      await sleep(2000);
    }
  }
  console.info('connected to runtime 2');

  kb1 = kb1 as KnowledgeBase;
  kb2 = kb2 as KnowledgeBase;

  console.info('registering ASK knowledge interaction in runtime 1');
  let ask = await kb1.registerAskKnowledgeInteraction(
    '?a <http://example.org/relatedTo> ?b .' // matching graph patterns
  ) as (b: any) => Promise<any>;
  
  console.info('registering ANSWER knowledge interaction in runtime 2');
  let answer = await kb2.registerAnswerKnowledgeInteraction(
    '?a <http://example.org/relatedTo> ?b .', // matching graph patterns
    (bindings: any) => {
      console.info('answering with knowledge from runtime 2');
      return [
        {a: '<http://example.org/Maths>', b: '<http://example.org/Science>'},
        {a: '<http://example.org/Books>', b: '<http://example.org/Magazines>'},
      ]
    }
  );

  console.info('asking for knowledge from runtime 1');
  let askResults = await ask([]);
  while (askResults.bindingSet.length == 0) {
    console.info('no results yet; retrying in 1 second');
    await sleep(1000);
    console.info('asking (again) for knowledge from runtime 1');
    askResults = await ask([]);
  }

  console.info('got results');
  console.info(askResults.bindingSet);

  console.info('cleaning up knowledge bases');
  await kb1.unregister();
  await kb2.unregister();

  console.info('bye');
}
