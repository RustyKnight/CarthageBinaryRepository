/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kaizen.cbr;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author swhitehead
 */
@WebServlet(name = "JsonGenerator", urlPatterns = {"/JsonGenerator"})
public class JsonGenerator extends HttpServlet {

    protected void downloadJson(List<String> parts, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // /json/{xcode version}/{library}/{tag}

        if (parts.size() < 3 || parts.size() > 4) {
            response.sendError(404, "Unknown resource");
            return;
        }
        String xcode = parts.get(1);
        String library = parts.get(2);
        String tag = "default";

        if (parts.size() == 3) {
            xcode = parts.get(1);
            library = parts.get(2);
        } else {
            tag = parts.get(3);
        }
        
        

        List<RepositoryManager.Binary> binaries = RepositoryManager.INSTANCE.getXcodeBinaries(getServletContext(), library, tag, xcode);
        if (binaries.size() == 0) {
            response.sendError(404, "No binaries avaliable for " + library + "/Xcode-" + xcode);
            return;
        }

        response.setContentType("application/json");

        StringJoiner sj = new StringJoiner(",\r\n\t", "{\r\n\t", "\n}");
        String path = "https://" + request.getServerName() + ":" + request.getServerPort() + "/binary/";
        for (RepositoryManager.Binary binary : binaries) {
            String name = library + "-v" + binary.getVesion() + "-Xcode" + xcode + "-framework.zip";

            sj.add("\"" + binary.getVesion() + "\": \""
                    + path
                    + library + "/" 
                    + tag + "/"
                    + binary.getVesion() + "/" 
                    + xcode + "/" 
                    + name + "\"");
        }

        try ( PrintWriter out = response.getWriter()) {
            out.println(sj.toString());
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        List<String> parts = new ArrayList<String>(Arrays.asList(path.split("\\/")));
        if (parts.size() > 0) {
            parts.remove(0);
        }
        if (parts.size() > 1 && parts.get(0).equals("json")) {
            downloadJson(parts, request, response);
        }

    }

}
