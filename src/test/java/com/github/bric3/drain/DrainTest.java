package com.github.bric3.drain;

import com.google.common.base.Splitter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DrainTest {

    @Test
    void smokeTest() throws IOException {
        var drain = new Drain("_");

        var lineCounter = new AtomicInteger();

        // "Jan  7 17:22:01 LabSZ sshd[30291]: "
        // "Jan  3 12:32:20 LabSZ sshd[941]: "

        Files.lines(Paths.get("build/resources/test/SSH.log"),
                    StandardCharsets.UTF_8)
             .peek(__ -> lineCounter.incrementAndGet())
             .map(l -> l.substring(l.indexOf("]: ") + 3)) // removes this part: "Dec 10 06:55:46 LabSZ sshd[24200]: "
             .forEach(content -> {
                 drain.parseLogMessage(content);
                 if (lineCounter.get() % 10000 == 0) {
                     System.out.printf("%4d clusters so far%n", drain.clusters().size());
                 }
             });

        drain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);
    }

/*
Python without masking:
=======================

--- Done processing file. Total of 655147 lines, rate 52853.2 lines/sec, 51 clusters
A0010 (size 140768): Failed password for <*> from <*> port <*> ssh2
A0009 (size 140701): pam unix(sshd:auth): authentication failure; logname= uid=0 euid=0 tty=ssh ruser= <*> <*>
A0007 (size 68958): Connection closed by <*> [preauth]
A0008 (size 46642): Received disconnect from <*> 11: <*> <*> <*>
A0014 (size 37963): PAM service(sshd) ignoring max retries; <*> > 3
A0012 (size 37298): Disconnecting: Too many authentication failures for <*> [preauth]
A0013 (size 37029): PAM <*> more authentication <*> logname= uid=0 euid=0 tty=ssh ruser= <*> <*>
A0011 (size 36967): message repeated <*> times: [ Failed password for <*> from <*> port <*> ssh2]
A0006 (size 20241): Failed <*> for invalid user <*> from <*> port <*> ssh2
A0004 (size 19852): pam unix(sshd:auth): check pass; user unknown
A0001 (size 18909): reverse mapping checking getaddrinfo for <*> <*> failed - POSSIBLE BREAK-IN ATTEMPT!
A0002 (size 14551): Invalid user <*> from <*>
A0003 (size 14551): input userauth request: invalid user <*> [preauth]
A0005 (size 14356): pam unix(sshd:auth): authentication failure; logname= uid=0 euid=0 tty=ssh ruser= <*>
A0018 (size 1289): PAM <*> more authentication <*> logname= uid=0 euid=0 tty=ssh ruser= <*>
A0024 (size 952): fatal: Read from socket failed: Connection reset by peer [preauth]
A0019 (size 930): error: Received disconnect from 103.99.0.122: 14: No more user authentication methods available. [preauth]
A0015 (size 838): Did not receive identification string from <*>
A0017 (size 592): Received disconnect from <*> 11: Closed due to user request. [preauth]
A0031 (size 497): Address <*> maps to <*> but this does not map back to the address - POSSIBLE BREAK-IN ATTEMPT!

A0020 (size 182): Accepted password for <*> from <*> port <*> ssh2
A0021 (size 182): pam unix(sshd:session): session opened for user <*> by (uid=0)
A0022 (size 182): pam unix(sshd:session): session closed for user <*>
A0016 (size 177): error: Received disconnect from <*> 3: com.jcraft.jsch.JSchException: Auth <*> [preauth]
A0032 (size 108): Received disconnect from <*> 11: [preauth]
A0050 (size 92): Received disconnect from 139.59.209.18: 11: Normal Shutdown, Thank you for playing [preauth]
A0042 (size 87): Received disconnect from <*> 11: <*> <*> <*> [preauth]
A0047 (size 60): Received disconnect from <*> 11: disconnect [preauth]
A0028 (size 30): Invalid user <*> <*> from <*>
A0029 (size 30): input userauth request: invalid user <*> <*> [preauth]
A0030 (size 30): Failed password for invalid user <*> <*> from <*> port <*> ssh2
A0036 (size 13): Invalid user from <*>
A0037 (size 13): input userauth request: invalid user [preauth]
A0038 (size 13): Failed <*> for invalid user from <*> port <*> ssh2
A0035 (size 8): Bad protocol version identification <*> <*> <*> from <*> port <*>
A0034 (size 7): Bad protocol version identification <*> <*> from <*> port <*>
A0039 (size 7): Bad protocol version identification <*> from <*> port <*>
A0033 (size 6): fatal: no hostkey alg [preauth]
A0040 (size 6): error: Received disconnect from <*> <*> <*> <*> <*> <*> [preauth]
A0044 (size 6): error: connect to <*> port 22: failed.
A0048 (size 6): Server listening on <*> port <*>
A0023 (size 3): fatal: Write failed: Connection reset by peer [preauth]
A0043 (size 3): error: Received disconnect from 195.154.45.62: 3: com.jcraft.jsch.JSchException: timeout in waiting for rekeying process. [preauth]
A0049 (size 3): Received disconnect from <*> 11: Disconnect requested by Windows SSH Client.
A0025 (size 2): error: Received disconnect from 191.96.249.68: 13: User request [preauth]
A0046 (size 2): error: Received disconnect from 212.83.176.1: 3: org.vngx.jsch.userauth.AuthCancelException: User authentication canceled by user [preauth]
A0026 (size 1): Bad packet length 1819045217. [preauth]
A0027 (size 1): Disconnecting: Packet corrupt [preauth]
A0041 (size 1): Received disconnect from 67.160.100.130: 11:
A0045 (size 1): Corrupted MAC on input. [preauth]
A0051 (size 1): syslogin perform logout: logout() returned an error

Java implementation:
====================

0011 (size=140768): Failed password for <*> from <*> port <*> ssh2
0010 (size=140701): pam unix(sshd:auth): authentication failure; logname= uid=0 euid=0 tty=ssh ruser= <*> <*>
0007 (size=68958): Connection closed by <*> [preauth]
0009 (size=46642): Received disconnect from <*> 11: <*> <*> <*>
0015 (size=37963): PAM service(sshd) ignoring max retries; <*> > 3
0013 (size=37298): Disconnecting: Too many authentication failures for <*> [preauth]
0014 (size=37029): PAM <*> more authentication <*> logname= uid=0 euid=0 tty=ssh ruser= <*> <*>
0012 (size=36967): message repeated <*> times: [ Failed password for <*> from <*> port <*> ssh2]
0004 (size=19852): pam unix(sshd:auth): check pass; user unknown
0006 (size=19818): Failed password for invalid user <*> from <*> port <*> ssh2
0001 (size=18909): reverse mapping checking getaddrinfo for <*> <*> failed - POSSIBLE BREAK-IN ATTEMPT!
0003 (size=14551): input userauth request: invalid user <*> [preauth]
0008 (size=14539): Invalid user <*> from <*>
0005 (size=14356): pam unix(sshd:auth): authentication failure; logname= uid=0 euid=0 tty=ssh ruser= <*>
0020 (size=1289): PAM <*> more authentication <*> logname= uid=0 euid=0 tty=ssh ruser= <*>
0026 (size=952): fatal: Read from socket failed: Connection reset by peer [preauth]
0021 (size=930): error: Received disconnect from 103.99.0.122: 14: No more user authentication methods available. [preauth]
0016 (size=838): Did not receive identification string from <*>
0018 (size=592): Received disconnect from <*> 11: Closed due to user request. [preauth]
0035 (size=497): Address <*> maps to <*> but this does not map back to the address - POSSIBLE BREAK-IN ATTEMPT!

0019 (size=423): Failed none for invalid user <*> from <*> port <*> ssh2
0022 (size=182): Accepted password for <*> from <*> port <*> ssh2
0023 (size=182): pam unix(sshd:session): session opened for user <*> by (uid=0)
0024 (size=182): pam unix(sshd:session): session closed for user <*>
0017 (size=177): error: Received disconnect from <*> 3: com.jcraft.jsch.JSchException: Auth <*> [preauth]
0036 (size=108): Received disconnect from <*> 11: [preauth]
0064 (size=92): Received disconnect from 139.59.209.18: 11: Normal Shutdown, Thank you for playing [preauth]
0050 (size=87): Received disconnect from <*> 11: <*> <*> <*> [preauth]
0056 (size=60): Received disconnect from <*> 11: disconnect [preauth]
0031 (size=30): input userauth request: invalid user <*> <*> [preauth]
0032 (size=30): Failed password for invalid user <*> <*> from <*> port <*> ssh2
0041 (size=13): Invalid user from <*>
0042 (size=13): input userauth request: invalid user [preauth]
0002 (size=12): Invalid user webmaster from <*>
0043 (size=9): Failed none for invalid user from <*> port <*> ssh2
0040 (size=8): Bad protocol version identification <*> <*> <*> from <*> port <*>
0038 (size=7): Bad protocol version identification <*> <*> from <*> port <*>
0044 (size=7): Bad protocol version identification <*> from <*> port <*>
0037 (size=6): fatal: no hostkey alg [preauth]
0046 (size=6): error: Received disconnect from <*> <*> <*> <*> <*> <*> [preauth]
0053 (size=6): error: connect to <*> port 22: failed.
0058 (size=6): Server listening on <*> port <*>
0030 (size=5): Invalid user myapn cen from 119.7.221.129
0034 (size=5): Invalid user web admin from 119.7.221.129
0049 (size=4): Invalid user ftp <*> from <*>
0052 (size=4): Failed password for invalid user from <*> port <*> ssh2
0025 (size=3): fatal: Write failed: Connection reset by peer [preauth]
0033 (size=3): Invalid user nvr admin from 119.7.221.129
0051 (size=3): error: Received disconnect from 195.154.45.62: 3: com.jcraft.jsch.JSchException: timeout in waiting for rekeying process. [preauth]
0062 (size=3): Received disconnect from <*> 11: Disconnect requested by Windows SSH Client.
0027 (size=2): error: Received disconnect from 191.96.249.68: 13: User request [preauth]
0039 (size=2): Invalid user back newshops from 42.159.145.29
0048 (size=2): Invalid user bash spm from 118.186.36.50
0055 (size=2): error: Received disconnect from 212.83.176.1: 3: org.vngx.jsch.userauth.AuthCancelException: User authentication canceled by user [preauth]
0059 (size=2): Invalid user lcap oracle from 119.7.221.129
0060 (size=2): Invalid user oem admin from 119.7.221.129
0061 (size=2): Invalid user zxdbm epg from 119.7.221.129
0028 (size=1): Bad packet length 1819045217. [preauth]
0029 (size=1): Disconnecting: Packet corrupt [preauth]
0045 (size=1): Invalid user ram k from 212.237.47.142
0047 (size=1): Received disconnect from 67.160.100.130: 11:
0054 (size=1): Corrupted MAC on input. [preauth]
0057 (size=1): Invalid user sugon test from 122.224.69.34
0063 (size=1): Invalid user super admin from 115.238.230.180
0065 (size=1): syslogin perform logout: logout() returned an error

*/
}