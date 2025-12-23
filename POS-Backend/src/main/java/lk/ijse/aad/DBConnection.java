package lk.ijse.aad;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.commons.dbcp2.BasicDataSource;

@WebListener
public class DBConnection implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver"); // or com.mysql.cj.jdbc.Driver
        ds.setUrl("jdbc:mysql://localhost:3306/pos_javaee");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setInitialSize(10);
        ds.setMaxTotal(50);

        // Use "dataSource" to match servlet
        servletContext.setAttribute("dataSource", ds);
    }
}
