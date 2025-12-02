package seoultech.se.client.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {
    /**
     * 로컬 사설 IP 주소를 반환합니다.
     * 루프백(127.0.0.1)이 아닌 실제 네트워크 인터페이스의 IP를 찾습니다.
     * 
     * @return 감지된 IP 주소 또는 실패 시 "127.0.0.1"
     */
    public static String getLocalIpAddress() {
        String selectedIp = "127.0.0.1";
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔍 [Network] Detecting ALL network interfaces...");
        System.out.println("=".repeat(70));
        
        java.util.List<String> allIps = new java.util.ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                // 모든 인터페이스 표시 (비활성 제외)
                if (!iface.isUp()) continue;
                
                boolean hasIp = false;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    
                    // IPv4만 처리
                    if (ip.indexOf(':') != -1) continue;
                    
                    if (!hasIp) {
                        System.out.println("\n📡 Interface: " + iface.getName() + " (" + iface.getDisplayName() + ")");
                        hasIp = true;
                    }
                    
                    System.out.println("   📍 IP: " + ip);
                    System.out.println("      Loopback: " + addr.isLoopbackAddress());
                    System.out.println("      Site Local: " + addr.isSiteLocalAddress());
                    
                    allIps.add(ip);
                    
                    // 루프백 제외
                    if (addr.isLoopbackAddress()) continue;
                    
                    // IP 선택 우선순위
                    // 1. RFC 1918 사설 IP (10.x, 172.16-31.x, 192.168.x)
                    // 2. 192.0.0.x (iOS hotspot/tethering)
                    boolean isPrivateIp = addr.isSiteLocalAddress() || ip.startsWith("192.0.0.");
                    
                    if (isPrivateIp) {
                        // 192.0.0.1 (게이트웨이)를 최우선으로 선택하고 고정
                        if (ip.equals("192.0.0.1")) {
                            selectedIp = ip;
                            System.out.println("      >>> GATEWAY IP (192.0.0.1) FOUND - Selected as primary!");
                        }
                        // 192.0.0.1이 이미 선택되었다면 다른 IP로 덮어쓰지 않음
                        else if (!selectedIp.equals("192.0.0.1")) {
                            // 아직 기본값이면 이 IP 선택
                            if (selectedIp.equals("127.0.0.1")) {
                                selectedIp = ip;
                                System.out.println("      >>> Selected as candidate IP");
                            }
                        }
                    }
                }
            }
            
            // 사설 IP를 못 찾은 경우, 루프백이 아닌 첫 번째 IPv4 주소 시도
            if (selectedIp.equals("127.0.0.1")) {
                interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        String ip = addr.getHostAddress();
                        if (!addr.isLoopbackAddress() && ip.indexOf(':') == -1) {
                            selectedIp = ip;
                            break;
                        }
                    }
                    if (!selectedIp.equals("127.0.0.1")) break;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌐 [Network] SELECTED IP: " + selectedIp);
        System.out.println("=".repeat(70));
        
        // 네트워크 환경 분석
        if (selectedIp.startsWith("192.0.0.")) {
            System.out.println("\n📱 iOS Hotspot/Tethering Network (192.0.0.x)");
            System.out.println("   All detected IPs: " + String.join(", ", allIps));
            
            if (allIps.contains("192.0.0.1")) {
                System.out.println("\n   ✅ HOTSPOT GATEWAY DETECTED (192.0.0.1)");
                if (selectedIp.equals("192.0.0.1")) {
                    System.out.println("   └ ✅ Correctly selected: " + selectedIp);
                    System.out.println("   └ ✅ You are providing the hotspot");
                    System.out.println("   └ ✅ Use P2P Host mode");
                    System.out.println("   └ 📢 Tell other players to connect to: 192.0.0.1");
                } else {
                    System.out.println("   └ ⚠️  WARNING: Wrong IP selected (" + selectedIp + ")");
                    System.out.println("   └ ⚠️  Should use 192.0.0.1 instead!");
                    System.out.println("   └ 💡 Manually enter 192.0.0.1 in Host field");
                }
            } else if (selectedIp.equals("192.0.0.2")) {
                System.out.println("   └ Single IP detected: " + selectedIp);
                System.out.println("   └ ⚠️  AMBIGUOUS SITUATION");
                System.out.println("   └ You could be:");
                System.out.println("      1) Connected to someone's hotspot → Guest role");
                System.out.println("      2) Using USB tethering → Check other player's IP");
                System.out.println("\n   💡 Action: Ask other player for their IP!");
                System.out.println("      - If they have 192.0.0.1 → You connect to them (Guest)");
                System.out.println("      - If they have 192.0.0.2 → Different networks! Use relay server");
            } else {
                System.out.println("   └ Unusual IP configuration");
                System.out.println("   └ Check with other players which IP to use");
            }
        } else if (selectedIp.startsWith("10.50.4")) {
            System.out.println("\n⚠️  School/Enterprise Network (10.50.x.x)");
            System.out.println("   └ Check if you're on the same subnet as other players");
            System.out.println("   └ Different subnets will NOT work for P2P");
        }
        System.out.println();
        
        return selectedIp;
    }
    
    /**
     * 두 IP 주소가 같은 서브넷에 있는지 확인합니다 (Class C 기준)
     */
    public static boolean isSameSubnet(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) return false;
        
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        
        if (parts1.length != 4 || parts2.length != 4) return false;
        
        // 처음 3개 옥텟(Class C) 비교
        boolean sameSubnet = parts1[0].equals(parts2[0]) && 
                             parts1[1].equals(parts2[1]) && 
                             parts1[2].equals(parts2[2]);
        
        return sameSubnet;
    }
    
    /**
     * P2P 연결 가능성을 체크하고 경고를 출력합니다
     */
    public static void checkP2PCompatibility(String myIp, String targetIp) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 P2P Connection Compatibility Check");
        System.out.println("=".repeat(60));
        System.out.println("Your IP:   " + myIp);
        System.out.println("Target IP: " + targetIp);
        System.out.println();
        
        if (myIp.equals(targetIp)) {
            System.out.println("❌ SAME IP DETECTED!");
            System.out.println("   Both devices have the same IP: " + myIp);
            System.out.println("   This is IMPOSSIBLE on the same network.");
            System.out.println();
            System.out.println("   Possible causes:");
            System.out.println("   1. You're checking your own IP (not the other player's)");
            System.out.println("   2. One is the hotspot provider, IP should be x.x.x.1");
            System.out.println("   3. Network configuration issue");
            System.out.println();
            System.out.println("   ⚠️  P2P CONNECTION WILL FAIL");
        } else if (!isSameSubnet(myIp, targetIp)) {
            System.out.println("❌ DIFFERENT SUBNETS!");
            System.out.println("   Your subnet:   " + getSubnet(myIp));
            System.out.println("   Target subnet: " + getSubnet(targetIp));
            System.out.println();
            System.out.println("   You are on DIFFERENT networks!");
            System.out.println("   P2P direct connection is NOT possible.");
            System.out.println();
            System.out.println("   Solutions:");
            System.out.println("   ✅ Connect to the SAME Wi-Fi network");
            System.out.println("   ✅ Use WebSocket relay server mode");
            System.out.println("   ✅ One device provides hotspot, other connects to it");
        } else {
            System.out.println("✅ SAME SUBNET - Connection possible!");
            System.out.println("   Subnet: " + getSubnet(myIp));
            System.out.println();
            System.out.println("   Additional checks:");
            
            // 모바일 핫스팟 체크
            if (myIp.startsWith("192.0.0.") || targetIp.startsWith("192.0.0.")) {
                System.out.println("   ⚠️  Mobile hotspot detected");
                System.out.println("      → Use WebSocket relay mode for better stability");
            }
            
            // 학교 네트워크 체크
            if (myIp.startsWith("10.50.") || targetIp.startsWith("10.50.")) {
                System.out.println("   ⚠️  School/Enterprise network detected");
                System.out.println("      → May have AP Isolation enabled");
                System.out.println("      → Try ping test first: ping " + targetIp);
            }
            
            System.out.println();
            System.out.println("   You can try P2P connection, but if it fails,");
            System.out.println("   use WebSocket relay mode instead.");
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
    
    /**
     * IP의 서브넷 주소를 반환합니다 (Class C)
     */
    private static String getSubnet(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "Invalid";
        return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
    }
}
