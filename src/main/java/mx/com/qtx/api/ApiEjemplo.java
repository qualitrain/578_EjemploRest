package mx.com.qtx.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import mx.com.qtx.entidades.Saludo;

@RestController
public class ApiEjemplo {
	private static int nPeticion = 0;
	private static final int PAUSA_MILIS = 2000;
		
	private static Logger log = LoggerFactory.getLogger(ApiEjemplo.class);
	
	@Autowired
	private Environment env;
	
	private String puerto = "";
	
	public ApiEjemplo() {
		super();
		log.info("ApiEjemplo instanciada");
	}
	
	private String getLogMonitoreoPeticion(String verbo, String uri) {
		String logMonitoreoPeticion = String.format(" [ (%d) %s %s, en Puerto:%s ]", nPeticion, verbo, uri, this.puerto);
		return logMonitoreoPeticion;
	}
	
	@PostConstruct
	public void inicializarApi() {
		this.puerto = this.env.getProperty("server.port");
	}

	private void hacerPausa(int milis) {
		try {
			Thread.sleep(milis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@GetMapping(path="/saludo", produces = MediaType.TEXT_PLAIN_VALUE)
	public String saludar() {
		nPeticion++;
		return "Buenas tardes" + this.getLogMonitoreoPeticion("GET", "/saludo");
	}
	
	@GetMapping(path="/saludo/{nombre}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String saludarA(@PathVariable String nombre) {
		nPeticion++;
		
		return "Buenas tardes " + nombre + " !!" + this.getLogMonitoreoPeticion("GET", "/saludo/" + nombre);
	}

	@GetMapping(path="/saludo/{nombre}/{n}", produces = MediaType.TEXT_PLAIN_VALUE)
	String saludarAnVeces(@PathVariable String nombre, @PathVariable int n) {	
		nPeticion++;
		String saludo = "";
		for(int i=0; i<n; i++) {
			saludo += "Buenas tardes " + nombre + ", ";
		}
		return saludo + this.getLogMonitoreoPeticion("GET", "/saludo/" + nombre + "/" + n);
	}
	
	@GetMapping(path="/saludo/json/{nombre}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Saludo generarSaludo(@PathVariable String nombre) {
		nPeticion++;
		Saludo saludo = new Saludo("Hola",nombre);
		saludo.setLogMonitoreo(this.getLogMonitoreoPeticion("GET", "/saludo/json/" + nombre));
		
		return saludo;
	
	}
	
	@GetMapping(path="/saludos", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Saludo> generarSaludos() {
		nPeticion++;
		String logMonitoreoPeticion = this.getLogMonitoreoPeticion("GET", "/saludos");
		
		if(nPeticion%3 == 0) { // Cada tercera peticion se pausara para probar mecanicas de resiliencia
			hacerPausa(PAUSA_MILIS);
		}		
		
		List<Saludo> listSaludos = new ArrayList<>();
		listSaludos.add(new Saludo("Hola","Betty", logMonitoreoPeticion));
		listSaludos.add(new Saludo("Buenos días","Jaime", logMonitoreoPeticion));
		listSaludos.add(new Saludo("Buenas tardes","Pedro", logMonitoreoPeticion));
		listSaludos.add(new Saludo("Buenas noches","Lorena", logMonitoreoPeticion));
		
		return listSaludos;
	
	}
	
	@GetMapping(path = "/saludo/xml/{nombre}",  
			produces = { MediaType.APPLICATION_XML_VALUE, 
					     MediaType.APPLICATION_JSON_VALUE})
	public Saludo generarSaludoXml(@PathVariable String nombre) {
		nPeticion++;
		Saludo saludo = new Saludo("Vientos!",nombre);
		
		saludo.setLogMonitoreo(this.getLogMonitoreoPeticion("GET", "/saludo/xml/" + nombre));
		return saludo;	
	}
	
	@GetMapping(path="/saludo/broma/{nombre}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Saludo> generarSaludoRE(@PathVariable String nombre) {
		nPeticion++;
		Saludo saludo = new Saludo("Hola",nombre);
		
		saludo.setLogMonitoreo(this.getLogMonitoreoPeticion("GET", "/saludo/broma/" + nombre));
		ResponseEntity<Saludo> response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				                                        .body(saludo);
		return response;	
	}
	
	@PostMapping(path = "/saludo", consumes = MediaType.APPLICATION_JSON_VALUE, 
			                       produces = MediaType.APPLICATION_XML_VALUE)
	public Saludo insertarSaludo(@RequestBody Saludo miSaludo) {
		nPeticion++;
		System.out.println("saludo recibido:" + miSaludo);
		miSaludo.setLogMonitoreo(this.getLogMonitoreoPeticion("POST", "/saludo"));
		return miSaludo;
	}

	@PostMapping(path = "/saludo2", consumes = MediaType.APPLICATION_JSON_VALUE, 
            produces = MediaType.APPLICATION_XML_VALUE)
	public Saludo insertarSaludoB(HttpEntity<Saludo> miSaludo, HttpServletRequest req) {
		nPeticion++;
		System.out.println("saludo recibido:" + miSaludo);
		System.out.println(miSaludo.getHeaders().getAccept());
		System.out.println("servlet path: " + req.getServletPath());
		
		Saludo saludo = miSaludo.getBody();
		saludo.setLogMonitoreo(this.getLogMonitoreoPeticion("POST", "/saludo2"));
		
		return miSaludo.getBody();
	}
	
	@ExceptionHandler
	public ResponseEntity<ErrorApi> manejarErrorDeTipoArgumento(MethodArgumentTypeMismatchException matmEx, HttpServletRequest req) {
		ErrorApi error = new ErrorApi(matmEx.getName(), 
				                      matmEx.getValue().toString(),
				                      "Error en el tipo de valor enviado. Debe ser un número",
				                      req.getServletPath());
		
		log.error("Petición con formato erróneo:" + error.toString());
		return new ResponseEntity<ErrorApi>(error, HttpStatus.NOT_ACCEPTABLE);
		
	}
	
	@ExceptionHandler
	public String manejarError(Exception ex) {
		String error = ex.getClass().getName() + ":" + ex.getMessage();
		return error;		
	}

}
