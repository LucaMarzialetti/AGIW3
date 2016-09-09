package it.uniroma3.agiw3.webinterface;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Servlet implementation class FirstPageAction
 */
@WebServlet("/processaQuery")
public class ActionSearch extends HttpServlet {
	private final static String elasticHost="localhost";
	private final static int elasticPort=9300;

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ActionSearch() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = request.getParameter("query");
		RequestDispatcher rd;
		ServletContext application = getServletContext();
		if(!query.isEmpty() && !query.equals("Cosa stai cercando?")){
			HttpSession session = request.getSession();
			session.setAttribute("query",query);
			Client client = TransportClient.builder().build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(elasticHost), elasticPort));
			try {
				SearchResponse resp = client.prepareSearch()
						.setQuery(
								QueryBuilders.queryStringQuery("*"+query+"*")
								.field("title^2")
								.field("description^1.5")
								.field("document"))
						.setSize(30)
						.execute()
						.actionGet();
				JSONObject obj = new JSONObject(resp);
				JSONObject hits = obj.getJSONObject("hits");
				JSONArray risultati = hits.getJSONArray("hits");

				session.setAttribute("total",  hits.getInt("totalHits"));
				session.setAttribute("result",  risultati);
				session.setAttribute("originalQuery", query);

			} 
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
			rd = application.getRequestDispatcher("/results.jsp");
		}
		else 
			rd = application.getRequestDispatcher("/searchPage.jsp");
		rd.forward(request, response);
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//POST-method
	}


}
