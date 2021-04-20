all: ClientCP1.java ClientCP2.java ServerCP1.java ServerCP2.java
	javac ClientCP1.java ClientCP2.java ServerCP1.java ServerCP2.java

cp1: ClientCP1.java ServerCP1.java
	javac ClientCP1.java ServerCP1.java

cp2: ClientCP2.java ServerCP2.java
	javac ClientCP2.java ServerCP2.java

original: ClientWithoutSecurity.java ServerWithoutSecurity.java
	javac ClientWithoutSecurity.java ServerWithoutSecurity.java

clean:
	rm -f ClientCP1.class ClientCP2.class ServerCP1.class ServerCP2.class ClientWithoutSecurity.class ServerWithoutSecurity.class AESKeyHelper.class RSAKeyHelper.class Utils.class PrivateKeyReader.class PublicKeyReader.class PacketTypes.class AESCipherHelper.class
	rm -rf upload/ download/