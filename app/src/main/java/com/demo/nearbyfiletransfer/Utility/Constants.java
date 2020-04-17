package com.demo.nearbyfiletransfer.Utility;

import java.util.HashMap;
import java.util.Map;
public class Constants {

    public static class ConnectionStatus{
        public static final int NEUTRAL = 10;
        public static final int REQUESTED = 11;
        public static final int CONNECTED = 12;
        public static final int REJECTED = 13;
    }

    public static class OperationCodes{
        public static final int compressOpCode = 41;
        public static final int grayscaleOpCode = 42;
        public static final String compressOperation = "Compress Image";
        public static final String grayscaleOperation = "Grayscale Image";
        public static int getOpCodeForOperation(String operation){
            int result=0;
            if(operation.equals(compressOperation)) result =compressOpCode;
            if(operation.equals(grayscaleOperation))    result= grayscaleOpCode;
            if(operation==null) result= -1;
            return result;
        }

    }
}
