package com.csquared.csgo.mapvoter;

import com.csquared.csgo.mapvoter.window.main.Main;

public class Launcher {

    public static String ip = "";

    public static void main(String[] args) {
        for(int i=0; i<args.length; i++)
            System.out.println(args[i]);
        Main.main(args);
    }

}
