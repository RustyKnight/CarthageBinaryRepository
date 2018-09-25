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
		
		if (parts.size() != 3) {
			response.sendError(404, "Unknown resource");
			return;
		}
		
//		json/10.0b10A255/KeychainAccess

		String xcode = parts.get(1);
		String library = parts.get(2);
		
		List<RepositoryManager.Binary> binaries = RepositoryManager.INSTANCE.getXcodeBinaries(getServletContext(), library, xcode);
		if (binaries.size() == 0) {
			response.sendError(404, "No binaries avaliable for " + library + "/Xcode-" + xcode);
			return;
		}

		response.setContentType("application/json");

		try (PrintWriter out = response.getWriter()) {
			out.println("{");
			String path = "https://" + request.getServerName() + ":" + request.getServerPort() + "/binary/";
			for (RepositoryManager.Binary binary : binaries) {
				String name = library + "-v" + binary.getVesion() + "-Xcode" + xcode + "-framework.zip";
				
				out.println("\t\"" + binary.getVesion() + "\": \"" + 
								path + 
								library + "/" + binary.getVesion() + "/" + xcode + "/" + name + "\"");
			}
			out.println("}");
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
