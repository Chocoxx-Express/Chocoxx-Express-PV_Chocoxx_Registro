module com.example.pv_pru_estructura_base {
    requires javafx.fxml;
    requires javafx.web;
    requires org.json;
    requires java.mail; // Módulo para usar JSONObject

    opens com.example.pv_pru_estructura_base to javafx.fxml;
    exports com.example.pv_pru_estructura_base;
}
