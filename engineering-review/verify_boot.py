import subprocess
import time
import urllib.request
import urllib.error
import sys
import os

def test_server():
    env = os.environ.copy()
    env["JAVA_HOME"] = "/opt/homebrew/opt/openjdk@21"
    
    print("Starting Spring Boot application via Maven...")
    proc = subprocess.Popen(
        ["./mvnw", "spring-boot:run", "-pl", "supplygraph-web"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        env=env
    )
    
    health_url = "http://localhost:8080/actuator/health"
    root_url = "http://localhost:8080/"
    
    success = False
    health_body = ""
    root_status = None
    root_html = ""
    
    start_time = time.time()
    try:
        while time.time() - start_time < 60:
            if proc.poll() is not None:
                print("Process terminated prematurely with code", proc.returncode)
                out, _ = proc.communicate()
                print("Output:\n", out[-2000:])
                return False
            try:
                with urllib.request.urlopen(health_url, timeout=2) as response:
                    if response.status == 200:
                        health_body = response.read().decode('utf-8')
                        print("Actuator Health Endpoint reached! Status 200")
                        success = True
                        break
            except Exception:
                time.sleep(1)
        
        if not success:
            print("Timed out waiting for server to become healthy.")
            return False
            
        print("\n--- Actuator Health Response ---")
        print(health_body)
        
        # Test Root Vaadin Page
        try:
            req = urllib.request.Request(root_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=5) as response:
                root_status = response.status
                root_html = response.read().decode('utf-8')
                print("\n--- Vaadin Root Page Status ---")
                print("Status Code:", root_status)
                print("HTML Preview (first 500 chars):")
                print(root_html[:500])
        except Exception as e:
            print("Failed to query root URL:", e)
            return False
            
        return True
    finally:
        print("\nTerminating Spring Boot process...")
        proc.terminate()
        try:
            proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()
        print("Spring Boot process stopped.")

if __name__ == "__main__":
    ok = test_server()
    if not ok:
        sys.exit(1)
    print("\nVERIFICATION OF GATE 5 & GATE 6 SUCCESSFUL!")

