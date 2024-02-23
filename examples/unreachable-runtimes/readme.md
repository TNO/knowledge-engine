## Knowledge Engine's distributed mode test
This docker compose project is used to test the Knowledge Engine's behavior in distributed mode when something exceptional happens (i.e. divergence from the happy flow). For example, one participant in the Knowledge Network configured its KER incorrectly and therefore it can reach out, but no one can contact the KER from the outside (via the Inter-KER protocol). Under such circumstances, we want the Knowledge Engine to keep functioning and behave as normal as possible.

To test this, we setup a distributed KER environment with 3 KER+KB combis that exchange data. We have `runtime-1+kb1`, `runtime-2+kb2` and `runtime-3+kb3`. By using the `iptables` tool for `runtime-3` we can simulate a misconfigured KER and test how the other Knowledge Engines behave. Use the following instructions to simulate the misconfigured KER. In the future we might want to use [Awall](https://github.com/alpinelinux/awall) instead of `iptables`.

Start the docker compose project: `docker compose up -d`

Retrieve the internal IP address of the KB3 (because it needs to always be able to contact it `runtime-3` we need its IP to make an exception in `iptables`). This is not really necessary if we use the hostname `kb3` of knowledge base 3 like we do below, but if you use an IP address there you should use the commands below to retrieve this IP. It changes everytime you restart the docker compose project.

```
> docker compose exec kb3 sh
> hostname -i
```

Make sure runtime-3 is configured to switch between being reachable to being unreachable. First open a shell for runtime-3.

```
docker compose exec runtime-3 sh
```

Configure `iptables-legacy` to allow the following packets to go through when we block incoming traffic:

```
iptables-legacy -A INPUT -i lo -j ACCEPT
iptables-legacy -A INPUT -p tcp -s kb3 -j ACCEPT
iptables-legacy -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
```

You can quickly test from inside a container whether another container that contains a KER is reachable from there using the following command: `wget -qO- http://runtime-3:8081/runtimedetails`

For example, if `iptables-legacy` is active and blocking all input traffic, you should no longer be able to receive JSON if you go to `runtime-2` and execute a `wget` to `runtime-3`, but you should be able to do the same from `runtime-3` to `runtime-2`.

You can quickly test from inside a container whether another container that contains a KER is reachable from there using the following command: `wget -qO- http://runtime-3:8081/runtimedetails`. You should receive some JSON that looks like:
 
```
 {"runtimeId":"http://runtime-3:8081","smartConnectorIds":["http://example.org/kb3"]}
```

Now, keep an eye on the log file with `docker compose logs -f` and use the following `iptables-legacy` commands to switch between unreachable and reachable.

```
iptables-legacy -P INPUT DROP
#runtime-3 is now unreachable for other KERs, but can still reach the Knowledge Directory (KD) and other KERs.

iptables-legacy -P INPUT ACCEPT
#runtime-3 is now reachable again for other KERs and can also reach the KD and other KERs.
```

Another scenario that you can check is when other KERs can access runtime-3, but it cannot send back a response to runtime-1. To do this, use the following filewall rule:

```
iptables-legacy -A OUTPUT -p tcp -d runtime-1 -m state --state NEW -j DROP
```