all: ClientWithSecurity.java ServerWithSecurity.java
	javac ClientWithSecurity.java ServerWithSecurity.java

original: ClientWithoutSecurity.java ServerWithoutSecurity.java
	javac ClientWithoutSecurity.java ServerWithoutSecurity.java

# We also delete and remove all the anonymous inner classes created and used in the ClientWithSecurity file by using the wildcard pattern (if any)
# This is because lambda expressions can only substitute anonymous inner classes/functional interfaces which have only a single non-default method
clean:
	rm -f ClientWithSecurity*.class ServerWithSecurity.class ClientWithoutSecurity.class ServerWithoutSecurity.class AESKeyHelper.class RSAKeyHelper.class Utils.class PrivateKeyReader.class PublicKeyReader.class PacketTypes.class AESCipherHelper.class SingleRootFileSystemView.class
	rm -rf upload/ download/
