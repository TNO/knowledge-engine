## Knowledge Engine's distributed mode test
This docker compose project is used to test the Knowledge Engine's behavior in distributed mode when something exceptional happens (i.e. divergence from the happy flow). For example, one participant in the Knowledge Network configured its KER incorrectly and therefore it can reach out, but no one can contact the KER from the outside (via the Inter-KER protocol). Under such circumstances, we want the Knowledge Engine to keep functioning and behave as normal as possible.

To test this, we setup a distributed KER environment with 3 KER+KB combis that exchange data. We have `runtime-1+kb1`, `runtime-2+kb2` and `runtime-3+kb3`. By using the `iptables` tool for `runtime-3` we can simulate a misconfigured KER and test how the other Knowledge Engines behave. Use the following instructions to simulate the misconfigured KER.

Start the docker compose project: `docker compose up -d`

Retrieve the internal IP address of the KB3 (because it needs to always be able to contact it `runtime-3` we need its IP to make an exception in `iptables`.
`docker compose exec kb3 sh`
`hostname -i`

Make sure runtime-3 is configured to switch between being reachable to being unreachable.
`docker compose exec runtime-3 bash`
`apt update -y`
`apt-get install iptables sudo -y`

Configure `iptables-legacy` to allow the following packets to go through when we block incoming traffic:

```
iptables-legacy -A INPUT -i lo -j ACCEPT
iptables-legacy -A INPUT -p tcp -s 172.32.5.6 -j ACCEPT
iptables-legacy -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
```

You can quickly test from inside a container whether another container that contains a KER is reachable from there using the following command: `wget -qO- http://runtime-3:8081/runtimedetails`

For example, if `iptables-legacy` is active and blocking all input traffic, you should no longer be able to receive JSON if you go to `runtime-2` and execute a `wget` to `runtime-3`, but you should be able to do the same from `runtime-3` to `runtime-2`.

You can quickly test from inside a container whether another container that contains a KER is reachable from there using the following command: `wget -qO- http://runtime-3:8081/runtimedetails`. You should receive some JSON that looks like:
 
 ```
 {"runtimeId":"http://runtime-3:8081","smartConnectorIds":["http://example.org/kb3"]}
 ```