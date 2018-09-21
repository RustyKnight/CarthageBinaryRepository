/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kaizen.cbr;

/**
 *
 * @author shanewhitehead
 */
public class Upload {
    public String libraryVersion;
    public String name;
    public String xcodeVersion;
    public String xcodeBuild;
    public String data;

    public Upload(String libraryVersion, String name, String xcodeVersion, String xcodeBuild, String data) {
        this.libraryVersion = libraryVersion;
        this.name = name;
        this.xcodeVersion = xcodeVersion;
        this.xcodeBuild = xcodeBuild;
        this.data = data;
    }
    
    
}
