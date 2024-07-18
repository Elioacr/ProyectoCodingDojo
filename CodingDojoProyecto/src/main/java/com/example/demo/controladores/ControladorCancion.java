package com.example.demo.controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.example.demo.modelos.Cancion;
import com.example.demo.modelos.Colaborador;
import com.example.demo.modelos.Usuario;
import com.example.demo.servicios.ServicioCancion;
import com.example.demo.servicios.ServicioColaborador;
import com.example.demo.servicios.ServicioUsuario;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class ControladorCancion {
	@Autowired
	private final ServicioCancion servicioCancion;
	private final ServicioUsuario servicioUsuario;
	private final ServicioColaborador servicioColaborador;

	public ControladorCancion(ServicioCancion servicioCancion, ServicioUsuario servicioUsuario, ServicioColaborador servicioColaborador) {
		this.servicioCancion = servicioCancion;
		this.servicioUsuario = servicioUsuario;
		this.servicioColaborador = servicioColaborador;
	}


	@GetMapping("/canciones")
	public String desplegarCancion(Model modelo,
								   HttpSession sesion) {
		if(sesion.getAttribute("id_usuario") == null) {
			return "redirect:/login";
		}

		List<Cancion> canciones = this.servicioCancion.obtenerTodos();
		modelo.addAttribute("canciones", canciones);

		return "canciones.jsp";
	}
	
	@GetMapping("/canciones/nuevo")
	public String desplegarFormularioCancion(@ModelAttribute("cancion") Cancion cancion,
											HttpSession sesion) {
		if(sesion.getAttribute("id_usuario") == null) {
			return "redirect:/login";
		}
		return "formularioCancion.jsp";
	}

	@PostMapping("/agregar/cancion")
	public String procesarAgregarCancion(@Valid @ModelAttribute("cancion") Cancion cancion,
	                                    BindingResult validaciones,
	                                    HttpSession sesion, Model modelo) {
	    if (validaciones.hasErrors()) {
	        return "formularioCancion.jsp";
	    }
	    
	    if (servicioCancion.existePorTitulo(cancion.getTitulo())) {
            modelo.addAttribute("errorTitulo", "El título de la canción ya existe.");
            return "formularioCancion.jsp";
        }

	    Long idUsuario = (Long) sesion.getAttribute("id_usuario");
	    Usuario usuario = this.servicioUsuario.obtenerPorId(idUsuario);
	    cancion.setUsuario(usuario);
	    servicioCancion.insertarCancion(cancion);
	    Colaborador colaborador = new Colaborador();
        colaborador.setUsuario(usuario);
        colaborador.setCancion(cancion);
        this.servicioColaborador.insertarColaborador(colaborador);

	    return "redirect:/canciones";
	} 
	

    @GetMapping("/canciones/{id}")
    public String verDetallesCancion(@PathVariable("id") Long id, Model model, HttpSession sesion) {
    	Cancion cancion = servicioCancion.obtenerPorId(id); 
        if(sesion.getAttribute("id_usuario") == null) {
			return "redirect:/login";
		}
        model.addAttribute("cancion", cancion);

        return "detalleCancion.jsp";
    }

    @GetMapping("/editar/cancion/{id}")
    public String desplegarEditarFormularioCancion(@ModelAttribute("programa") Cancion cancion,
                                                    @PathVariable("id") Long id,
                                                    Model modelo,
                                                    HttpSession sesion) {
        if(sesion.getAttribute("id_usuario") == null) {
            return "redirect:/login";
        }
        cancion = this.servicioCancion.obtenerPorId(id);
        cancion.setLetraOriginal(cancion.getLetra());
        cancion.setLetra("");
        modelo.addAttribute("cancion", cancion);
        return "editarCancion.jsp";
    }

    @PutMapping("/procesar/editar/cancion/{id}")
    public String procesaEditarCancion(@Valid @ModelAttribute("cancion") Cancion cancion,
                                        BindingResult validaciones,
                                        @PathVariable("id") Long id,
                                        HttpSession sesion) {
        if(validaciones.hasErrors()) {
            return "editarPrograma.jsp";
        }
        
        Cancion cancionNueva = servicioCancion.obtenerPorId(id);
        Long idUsuario = (Long)sesion.getAttribute("id_usuario");
        Usuario usuario = this.servicioUsuario.obtenerPorId(idUsuario);
        if(! cancionNueva.getColaboradores().stream().anyMatch(colaborador -> usuario.getId().equals(colaborador.getUsuario().getId()))) {
        	Colaborador colaborador = new Colaborador();
        	colaborador.setUsuario(usuario);
        	colaborador.setCancion(cancionNueva);
        	this.servicioColaborador.insertarColaborador(colaborador);
        }
        
        cancion.setId(id);
        cancion.setUsuario(usuario);
        cancion.setLetra(cancion.getLetraOriginal()+cancion.getLetra());
        this.servicioCancion.actualizarCancion(cancion);
        
        return "redirect:/canciones";
    }
    
    @DeleteMapping("/eliminar/cancion/{id}")
    public String eliminarCancion(@PathVariable("id") Long id, HttpSession sesion) {
    	Cancion cancion = servicioCancion.obtenerPorId(id);
    	if(cancion.getUsuario().getId() == sesion.getAttribute("id_usuario")) {
    		servicioCancion.eliminarPorId(id);    		
    	}
    	
        return "redirect:/canciones";
    }

	@GetMapping("/logout")
	public String procesaLogout(HttpSession sesion) {
		sesion.invalidate();
		return "redirect:/login";
	}

}