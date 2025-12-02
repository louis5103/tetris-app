package seoultech.se.client.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {
    /**
     * 로컬 사설 IP 주소를 반환합니다.
     * 루프백(127.0.0.1)이 아닌 실제 네트워크 인터페이스의 IP를 찾습니다.
     * 
     * @return 감지된 IP 주소 또는 실패 시 "127.0.0.1"
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 루프백이나 비활성 인터페이스, 가상 인터페이스 무시
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // IPv4 주소이면서 site-local(사설 IP)인 경우 선호
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        System.out.println("🌐 [Network] Selected IP: " + addr.getHostAddress() + " (interface: " + iface.getName() + ")");
                        return addr.getHostAddress();
                    }
                }
            }
            
            // 사설 IP를 못 찾은 경우, 루프백이 아닌 첫 번째 IPv4 주소 시도
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        System.out.println("🌐 [Network] Selected IP: " + addr.getHostAddress() + " (interface: " + iface.getName() + ")");
                        return addr.getHostAddress();
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("⚠️ [Network] Could not detect network IP, using fallback 127.0.0.1");
        return "127.0.0.1"; // Fallback
    }
    
    /**
     * 모든 사용 가능한 네트워크 인터페이스 정보를 출력합니다.
     * 디버깅 용도입니다.
     */
    public static void printNetworkInterfaces() {
        System.out.println("\n=== Available Network Interfaces ===");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                System.out.println("\nInterface: " + iface.getName());
                System.out.println("  Display Name: " + iface.getDisplayName());
                System.out.println("  Up: " + iface.isUp());
                System.out.println("  Loopback: " + iface.isLoopback());
                System.out.println("  Virtual: " + iface.isVirtual());
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    System.out.println("    Address: " + addr.getHostAddress());
                    System.out.println("      Site Local: " + addr.isSiteLocalAddress());
                    System.out.println("      Link Local: " + addr.isLinkLocalAddress());
                    System.out.println("      Loopback: " + addr.isLoopbackAddress());
                }
            }
            System.out.println("=====================================\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
