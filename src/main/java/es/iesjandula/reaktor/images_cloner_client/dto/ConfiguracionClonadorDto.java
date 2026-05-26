package es.iesjandula.reaktor.images_cloner_client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la configuración del clonador
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfiguracionClonadorDto
{
    /** Si está a true es que se debe mostrar el menú del clonador */
    private boolean menuActivo ;

    /** Si está a true es que se debe activar la imagen */
    private boolean activarImagen ;

    /** Nombre de la imagen que se debe activar */
    private String nombreImagen ;

    /** Acción post-restore: poweroff, reboot o true */
    private String accion ;
}
