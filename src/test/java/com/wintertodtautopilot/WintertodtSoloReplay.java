package com.wintertodtautopilot;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local developer tool: ./gradlew replaySolo -PtraceFile=/absolute/path/to/client.log */
public final class WintertodtSoloReplay
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) { throw new IllegalArgumentException("Provide one trace file path"); }
        WintertodtSoloCapture.Replay replay = new WintertodtSoloCapture.Replay();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8))
        {
            String line; int number = 0;
            while ((line = reader.readLine()) != null)
            {
                number++;
                if (!line.contains(WintertodtSoloCapture.PREFIX)) { continue; }
                try { replay.accept(WintertodtSoloCapture.decode(line)); }
                catch (RuntimeException e) { throw new IllegalStateException("Replay failed on line " + number, e); }
            }
        }
        if (replay.getDecisions() == 0) { throw new IllegalArgumentException("No Solo capture records found"); }
        System.out.println("Replayed " + replay.getDecisions() + " Solo decisions with matching state, objectives and phases.");
    }
}
