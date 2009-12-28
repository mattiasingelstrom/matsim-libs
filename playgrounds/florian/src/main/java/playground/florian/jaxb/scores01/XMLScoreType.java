//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.07.02 at 09:31:38 AM MESZ 
//


package playground.florian.jaxb.scores01;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.matsim.jaxb.Adapter1;


/**
 * <p>Java class for scoreType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="scoreType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="iteration" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger"/>
 *         &lt;element name="averageExecuted" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="averageWorst" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="averageAverage" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="averageBest" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "scoreType", propOrder = {
    "iteration",
    "averageExecuted",
    "averageWorst",
    "averageAverage",
    "averageBest"
})
public class XMLScoreType {

    @XmlElement(required = true, type = String.class)
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected Integer iteration;
    protected double averageExecuted;
    protected double averageWorst;
    protected double averageAverage;
    protected double averageBest;

    /**
     * Gets the value of the iteration property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getIteration() {
        return iteration;
    }

    /**
     * Sets the value of the iteration property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIteration(Integer value) {
        this.iteration = value;
    }

    /**
     * Gets the value of the averageExecuted property.
     * 
     */
    public double getAverageExecuted() {
        return averageExecuted;
    }

    /**
     * Sets the value of the averageExecuted property.
     * 
     */
    public void setAverageExecuted(double value) {
        this.averageExecuted = value;
    }

    /**
     * Gets the value of the averageWorst property.
     * 
     */
    public double getAverageWorst() {
        return averageWorst;
    }

    /**
     * Sets the value of the averageWorst property.
     * 
     */
    public void setAverageWorst(double value) {
        this.averageWorst = value;
    }

    /**
     * Gets the value of the averageAverage property.
     * 
     */
    public double getAverageAverage() {
        return averageAverage;
    }

    /**
     * Sets the value of the averageAverage property.
     * 
     */
    public void setAverageAverage(double value) {
        this.averageAverage = value;
    }

    /**
     * Gets the value of the averageBest property.
     * 
     */
    public double getAverageBest() {
        return averageBest;
    }

    /**
     * Sets the value of the averageBest property.
     * 
     */
    public void setAverageBest(double value) {
        this.averageBest = value;
    }

}
