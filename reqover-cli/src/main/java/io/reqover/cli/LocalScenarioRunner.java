/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.cli;

import io.reqover.report.TestScenario;
import io.reqover.report.ScenarioRun;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicitly consented, bounded sequential local checks. No proxies, redirects or credentials. */
final class LocalScenarioRunner {
    private LocalScenarioRunner() { }
    static URI validate(TestScenario scenario,URI origin,int repeat,boolean confirmed) {
        scenario.requireRunnable();
        if(!confirmed)throw new IllegalArgumentException("--confirm-local is required; GET can have side effects");
        if(!Set.of("http","https").contains(origin.getScheme()) || !Set.of("127.0.0.1","[::1]").contains(origin.getHost()==null?"":origin.getHost())
          || origin.getRawUserInfo()!=null || origin.getRawQuery()!=null || origin.getRawFragment()!=null
          || (!origin.getRawPath().isEmpty() && !origin.getRawPath().equals("/")) || origin.getPort()>65535 || origin.getPort()==0) {
            throw new IllegalArgumentException("Explicit literal loopback HTTP(S) origin required (no credentials/path/query/fragment)");
        }
        if(repeat<1 || repeat>10 || repeat*scenario.cases().size()>1000)throw new IllegalArgumentException("Repeat must be 1-10, at most 1000 attempts");
        return URI.create(origin.getScheme()+"://"+origin.getRawAuthority());
    }
    static ScenarioRun run(TestScenario scenario,URI origin,int repeat,boolean confirmed) {
        URI base=validate(scenario,origin,repeat,confirmed);
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER)
          .proxy(new ProxySelector() { public List<Proxy> select(URI ignored){return List.of(Proxy.NO_PROXY);} public void connectFailed(URI uri,SocketAddress address,java.io.IOException error){} }).build();
        var attempts=new ArrayList<ScenarioRun.Attempt>(); String started=Instant.now().toString(); long beginning=System.nanoTime();
        String completion="COMPLETE";
        outer: for(int iteration=0;iteration<repeat;iteration++)for(var c:scenario.cases()) {
            long remaining=TimeUnit.SECONDS.toNanos(60)-(System.nanoTime()-beginning);
            if(remaining<=0){completion="BUDGET_EXHAUSTED";break outer;}
            if(Thread.currentThread().isInterrupted()){completion="INTERRUPTED";break outer;}
            Integer status=null; String outcome="NETWORK_ERROR";long start=System.nanoTime();
            var request=HttpRequest.newBuilder(base.resolve(c.path())).timeout(Duration.ofSeconds(10)).method(c.method(),HttpRequest.BodyPublishers.noBody()).build();
            var future=client.sendAsync(request,HttpResponse.BodyHandlers.discarding());
            try { status=future.get(Math.min(remaining,TimeUnit.SECONDS.toNanos(10)),TimeUnit.NANOSECONDS).statusCode(); outcome="PASS"; }
            catch(TimeoutException e){future.cancel(true);outcome="TIMEOUT";}
            catch(InterruptedException e){future.cancel(true);Thread.currentThread().interrupt();outcome="INTERRUPTED";completion="INTERRUPTED";}
            catch(ExecutionException e){future.cancel(true);outcome="NETWORK_ERROR";}
            double elapsed=(System.nanoTime()-start)/1_000_000.0;
            if(status!=null){outcome=!c.expectedStatus().equals(status)?"STATUS_MISMATCH":c.maximumClientMillis()!=null && elapsed>c.maximumClientMillis()?"CLIENT_LIMIT_EXCEEDED":"PASS";}
            attempts.add(new ScenarioRun.Attempt(c.id(),status,elapsed,outcome));
            if(completion.equals("INTERRUPTED"))break outer;
        }
        return new ScenarioRun(UUID.randomUUID().toString(),scenario,base.toString(),started,Instant.now().toString(),repeat*scenario.cases().size(),completion,attempts);
    }
}
