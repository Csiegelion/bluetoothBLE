/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.appli_ISCAPI;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */

    // On ne se sert pas de ces données, cependant nous avons decidé de la conserver pour l'amélioration future de l'application
    // Par exemple, si vous souhaitez travailler sur plusieurs services, vous pourrez les définir proprement ici.
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("00000000-0001-11e1-9ab4-0002a5d5c51b", "device Service");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Service1");
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Service2");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00140000-0001-11e1-ac36-0002a5d5c51b", "Characteristics read");
        attributes.put("00c00000-0001-11e1-ac36-0002a5d5c51b", "Characteristics unknown");
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "Characteristics une");
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Characteristics deux");
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Characteristics trois");
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "Characteristics quarter");
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
