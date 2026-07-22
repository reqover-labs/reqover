package sample.agent;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.ReqoverProbe;

public final class AgentSmokeMain {
    private AgentSmokeMain() {
    }

    public static void main(String[] args) {
        ReqoverProbe.resetGlobalStateForTests();

        String value = new AgentSmokeTarget().work("reqover");
        if (!"agent:reqover".equals(value)) {
            System.err.println("unexpected target result: " + value);
            System.exit(2);
        }

        boolean observed = ProbeRegistry.all().stream().anyMatch(AgentSmokeMain::hasGlobalHit);
        if (!observed) {
            System.err.println("agent did not record target probe hit");
            System.err.println("registered metadata=" + ProbeRegistry.all());
            System.err.println("global snapshot=" + ReqoverProbe.globalSnapshot());
            System.exit(3);
        }

        System.out.println("agent smoke ok");
    }

    private static boolean hasGlobalHit(ProbeMetadata metadata) {
        return metadata.className().equals("sample.agent.AgentSmokeTarget")
                && ReqoverProbe.globalSnapshot().hasHit(metadata.classId(), metadata.probeId());
    }
}

