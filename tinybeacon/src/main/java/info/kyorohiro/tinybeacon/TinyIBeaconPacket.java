package info.kyorohiro.tinybeacon;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kyorohiro on 2016/02/16.
 */
public class TinyIBeaconPacket {
    static public final int PROXIMITY_NONE = 0;
    static public final int PROXIMITY_IMMEDIATE = 1;
    static public final int PROXIMITY_NEAR = 2;
    static public final int PROXIMITY_FAR = 3;
    static public final int PROXIMITY_UNKNOWN = 4;

    static public int getProximity(TinyAdPacket packet, double rssi, int currentState) {
        double d = distance(packet, rssi);
        if(d < 0.25) {
            return PROXIMITY_IMMEDIATE;
        }
        if(currentState == PROXIMITY_IMMEDIATE || currentState == PROXIMITY_NEAR) {
            if(d < 1.95) {
                return PROXIMITY_NEAR;
            } else {
                return PROXIMITY_FAR;
            }
        } else {
            if(d < 1.25) {
                return PROXIMITY_NEAR;
            } else {
                return PROXIMITY_FAR;
            }
        }
    }

    //
    // TODO create iBeacon class & functionry method
    //
    static public boolean isIBeacon(TinyAdPacket packet) {
        if(packet.getAdType() != TinyAdPacket.ADTYPE_MANUFACTURE_SPECIFIC) {
            return false;
        }
        //
        if(packet.getDataLength() != 0x1A) {
            return false;
        }
        byte[] cont = packet.getContent();
        //
        // 004c is apple
        if(!(cont[0] == 0x4C && cont[1] == 0x00)) {
            return false;
        }
        return true;
    }

    //
    // TODO create iBeacon class & functionry method
    //
    static public byte[] makeIBeaconAdvertiseData(byte[] uuid, int major, int minor, int txPower) {
        byte[] cont = new byte[0x1a-1-2];
        //
        // 004c is apple
        //
        //
        //cont[0] = 0x4C;
        //cont[1] = 0x00;
        // indicator
        cont[2-2] = 0x02;
        cont[3-2] = 0x15;
        // uuid
        System.arraycopy(uuid,0,cont,4-2,16);
        //
        //major
        ByteBuffer majorBuffer = ByteBuffer.allocate(2).putShort((short)major);
        cont[20-2] = majorBuffer.get(1);
        cont[21-2] = majorBuffer.get(0);
        ByteBuffer minorBuffer = ByteBuffer.allocate(2).putShort((short)major);
        cont[22-2] = minorBuffer.get(1);
        cont[23-2] = minorBuffer.get(0);
        cont[24-2] = (byte)txPower;
        return cont;
    }

    //
    // RSSI = Power - 20  log10(100cm);
    // -(RSSI - Power)/20 =  log10(d);
    // 10 ^(Power -RSSI)/20 = d : d is per 100cm
    //
    // DISTANCE = 10 ^ ((POWER-RSSI)/20)
    // RSSI     = POWER - 20*log10(D) : about d --> {100cm --> 1 , 50cm -->0.5, 200cm --> 2}
    //
    // 50cm  a+6
    // 100cm a
    // 200cm a-6
    // 400cm a-12
    static public double distance(TinyAdPacket packet, double rssi) {
        return distance (getCalibratedRSSIAsIBeacon(packet),-rssi);
    }

    static public double distance(int txPower, double rssi) {
        return Math.pow(10.0, (txPower-rssi)/20.0 );
    }

    static public int getIdentifierAsIBeacon_00(TinyAdPacket packet) {
        return 0xff & packet.getContent()[2];
    }

    static public int getIdentifierAsIBeacon_01(TinyAdPacket packet) {
        return 0xff & packet.getContent()[3];
    }

    static public byte[] getUUIDAsIBeacon(TinyAdPacket packet) {
        byte[] cont = packet.getContent();
        byte[] ret = new byte[16];
        System.arraycopy(cont,4,ret,0,ret.length);
        return ret;
    }

    static public int getMajorAsIBeacon(TinyAdPacket packet) {
        byte[] cont = packet.getContent();
        return ByteBuffer.wrap(cont,20,2).getShort();
    }

    static public int getMinorAsIBeacon(TinyAdPacket packet) {
        byte[] cont = packet.getContent();
        return ByteBuffer.wrap(cont,22,2).getShort();
    }


    static public int getCalibratedRSSIAsIBeacon(TinyAdPacket packet) {
        return packet.getContent()[24];
    }

    static public String getUUIDHexStringAsIBeacon(TinyAdPacket packet) {
        return getUUIDHexStringAsIBeacon(getUUIDAsIBeacon(packet));
    }

    static public String getUUIDHexStringAsIBeacon(byte[] cont) {
        StringBuilder builder = new StringBuilder();
        for(byte c : cont) {
            if(0xF < (c&0xff)) {
                builder.append(Integer.toHexString(0xff&c));
            } else {
                builder.append("0");
                builder.append(Integer.toHexString(0xff&c));
            }
        }
        return builder.toString();
    }

    static public byte[] getUUIDBytesAsIBeacon(String uuid) {
        byte[] ret = new byte[16];
        StringBuilder builder = new StringBuilder();
        for(int i=0,j=0;j<uuid.length()&& i<ret.length;) {
            if(vMap.containsKey(uuid.charAt(j))) {
                ret[i] = (byte) (0x10 * vMap.get(uuid.charAt(j)) + vMap.get(uuid.charAt(j + 1)));
                i++;
                j+=2;
            } else {
                j++;
            }
        }
        return ret;
    }

    static private Map<Character,Integer> vMap = new HashMap<Character,Integer>(){{
        put('0',0);
        put('1',1);
        put('2',2);
        put('3',3);
        put('4',4);
        put('5',5);
        put('6',6);
        put('7',7);
        put('8',8);
        put('9',9);
        put('a',10);
        put('b',11);
        put('c',12);
        put('d',13);
        put('e',14);
        put('f',15);
        put('A',10);
        put('B',11);
        put('C',12);
        put('D',13);
        put('E',14);
        put('F',15);
    }};
}
