package presentacio;

import dades.ComandaDAO;
import dades.ProducteDAO;
import entitats.Comanda;
import entitats.ComandaLogic;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import java.sql.Date;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.StringConverter;

/**
 * Controlador de la vista 'comandes.fxml'. Permet a l'usuari gestionar el CRUD
 * d'una capçalera de comanda des d'una UI.
 *
 * @author Txell Llanas - Creació vista FXML/Implementació
 * @author Pablo Morante - Creació/Implementació
 * @author Victor García - Creació/Implementació
 */
public class ComandesController implements Initializable {

    @FXML
    private Button btnOrders;
    @FXML
    private Button btnCustomers;
    @FXML
    private Button btnProducts;
    @FXML
    private Button btnAbout;
    @FXML
    private Button btnNewOrder;
    @FXML
    private DatePicker datePickerFrom;
    @FXML
    private DatePicker datePickerTo;
    @FXML
    private TableView<Comanda> ordersList;
    @FXML
    private TableColumn columnOrderNumber;
    @FXML
    private TableColumn columnRequiredDate;
    @FXML
    private TableColumn columnShippedDate;
    @FXML
    private TableColumn columnCustomerEmail;
    @FXML
    private TableColumn columnOrderAmount;
    @FXML
    private TableColumn<Comanda, Comanda> columnActions;
    @FXML
    private Button ClearButton;

    private final Tooltip tooltipEliminar = new Tooltip("Eliminar Comanda");

    private final Tooltip tooltipDetail = new Tooltip("Veure Comanda");

    // Definir una llista observable d'objectes de tipus Client
    private final ObservableList<Comanda> llistaObservableComanda = FXCollections.observableArrayList();

    // Instància de ComandaDAO per carregar els registres de la taula 'orders'
    private ComandaDAO dataComanda;

    // Instància de ComandaLogic per carregar els mètodes de validacions
    private final ComandaLogic validate = new ComandaLogic();

    /**
     * comandaActual
     */
    public static Comanda comandaActual;

    /**
     * Inicialitza els components especificats.
     *
     * @param url url
     * @param rb rb
     * @author Txell Llanas - Creació/Implementació
     * @author Pablo Morante - Implementació
     * @author Víctor García - Implementació
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Obtenir el texte dels botons
        String text1 = btnOrders.getText();
        String text2 = btnCustomers.getText();
        String text3 = btnProducts.getText();
        String text4 = btnAbout.getText();
        String text5 = btnNewOrder.getText();

        // Passar el texte a MAJÚSCULES
        btnOrders.setText(text1.toUpperCase());
        btnCustomers.setText(text2.toUpperCase());
        btnProducts.setText(text3.toUpperCase());
        btnAbout.setText(text4.toUpperCase());
        btnNewOrder.setText(text5.toUpperCase());

        // Definir format:(dia/mes/any) a mostrar quan s'edita a dins els camps dels calendaris
        String datePattern = "dd/MM/yyyy";                                      // Format per aplicar a la Data
        datePickerFrom.setPromptText("Data inicial");                           // Texte que es mostra al camp Data
        datePickerTo.setPromptText("Data final");                               // Texte que es mostra al camp Data 

        datePickerFrom.setConverter(new StringConverter<LocalDate>() {

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);                          // Aplico format a la data
                } else {
                    return "";
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);              // Aplico format a la data
                } else {
                    return null;
                }
            }
        });
        datePickerTo.setConverter(new StringConverter<LocalDate>() {

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);                          // Aplico format a la data
                } else {
                    return "";
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);              // Aplico format a la data
                } else {
                    return null;
                }
            }
        });

        //si no existeixen productes, no es pot crear una comanda
        try {
            btnNewOrder.setDisable(!(!new ProducteDAO().getAll().isEmpty()));
        } catch (SQLException ex) {

        }

        // Recuperar registres taula 'orders'
        fillOrdersTable();

        //Recuperar registres entre dues dates
        filteredTable();
    }

    /**
     * Mostra un formulari per 'Crear' o 'Editar' una comanda.
     *
     * @throws IOException Excepció a mostrar en cas que no es trobi el Layout
     * @author Txell Llanas - Creació
     * @author Pablo Morante - Implementació
     * @author Víctor García - Implementació
     */
    @FXML
    private void goToNewOrder() throws IOException {

        // Carregar la vista del formulari "COMANDES (Detalls)" in-situ
        setComandaActual(new Comanda(0)); // si la comanda és nova, comanda nova amb id 0
        App.setRoot("comandesForm");
    }

    /**
     * Mètode que recupera tots els registres de la taula 'orders'.
     *
     * @author Pablo Morante - Creació/Implementació
     * @author Víctor García - Creació/Implementació
     */
    private void fillOrdersTable() {

        try {

            dataComanda = new ComandaDAO();

            llistaObservableComanda.addAll(dataComanda.getAll());

            // Vincular els atributs de Comanda amb cada columna de la taula per mostrar les dades recuperades dins el tableview:
            columnOrderNumber.setCellValueFactory(new PropertyValueFactory<>("numOrdre"));
            columnRequiredDate.setCellValueFactory(new PropertyValueFactory<Comanda, Date>("dataEntrega"));
            columnShippedDate.setCellValueFactory(new PropertyValueFactory<Comanda, Date>("dataEnviament"));
            columnCustomerEmail.setCellValueFactory(new PropertyValueFactory<>("customers_customerEmail"));
            columnOrderAmount.setCellValueFactory(new PropertyValueFactory<>("total"));

            columnActions.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            addCellButtons(); // Afegir BOTONS ACCIONS

            // Aplicar estils pels camps NO EDITABLES
            columnOrderNumber.setCellFactory(tc -> new TableCell<Comanda, Integer>() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected void updateItem(Integer value, boolean empty) {
                    super.updateItem(value, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        getStyleClass().add("non-editable");
                        setText(Integer.toString(value));
                    }
                }
            });

            // Aplicar format de 2 decimals + símbnol monetari
            columnOrderAmount.setCellFactory(tc -> new TableCell<Comanda, Number>() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected void updateItem(Number value, boolean empty) {
                    super.updateItem(value, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", Float.parseFloat(value.toString())) + " €");
                        setGraphic(null);
                    }
                }
            });

            //makeColsEditable();
            // Afegir els registres a la taula
            ordersList.setItems(llistaObservableComanda);

        } catch (SQLException ex) {
            System.out.println("S'ha produit un error a la BD.");
        }
    }

    /**
     * Mètode que afegeix botons dins la cel·la d'accions de la TableView
     *
     * @author Víctor García - Creació/Implementació
     */
    private void addCellButtons() {

        columnActions.setCellFactory(param -> new TableCell<Comanda, Comanda>() {

            private final Button btnDetail = new Button("");
            private final Button btnDelete = new Button("");
            private final HBox container = new HBox(btnDetail, btnDelete);

            /**
             * {@inheritDoc}
             */
            @Override
            protected void updateItem(Comanda t, boolean empty) {
                super.updateItem(t, empty);

                if (t == null) {
                    setGraphic(null);
                    return;
                }

                // Inserir container amb botons a dins
                setGraphic(container);
                container.setId("container");
                container.setAlignment(Pos.CENTER);

                // Desar canvis registre actual
                btnDetail.setId("btnDetail");
                btnDetail.setTooltip(tooltipDetail);
                btnDetail.setOnAction(event -> {
                    try {
                        goToOrderDetails(t);
                    } catch (IOException ex) {
                        System.out.println("S'ha produit un error d'interficie.");
                    }
                });

                // FALTA IF/ELSE PARA SI LA COMANDA TIENE PRODUCTOS DENTRO NO PODER BORRARLA
                btnDelete.setId("btnDelete");
                btnDelete.setTooltip(tooltipEliminar);
                btnDelete.setOnAction(event -> {

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("AVÍS");
                    alert.setHeaderText("Estàs a punt d'eliminar la comanda \"" + t.getNumOrdre() + "\". Vols continuar?");

                    ButtonType yesButton = new ButtonType("Sí");
                    ButtonType cancelButton = new ButtonType("No");

                    alert.getButtonTypes().setAll(yesButton, cancelButton);

                    if (alert.showAndWait().get() == yesButton) {
                        try {
                            dataComanda.delete(t);
                            llistaObservableComanda.remove(t);
                        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
                            alert.setHeaderText("Una comanda amb productes no es pot eliminar. Vols eliminar tots els productes d'aquesta la comanda?");
                            if (alert.showAndWait().get() == yesButton) {
                                dataComanda.deleteAllProductsFromComanda(t.getNumOrdre());
                                try {
                                    dataComanda.delete(t);
                                    llistaObservableComanda.remove(t);
                                } catch (SQLException ex1) {
                                    Logger.getLogger(ComandesController.class.getName()).log(Level.SEVERE, null, ex1);
                                }
                            } else {
                                alert.close();
                            }
                        } catch (SQLException ex) {
                            System.out.println("S'ha produit un error de BD.");
                        }
                    } else {
                        alert.close();
                    }
                });
            }
        }
        );
    }

    /**
     * Mostra el detall de la comanda que hem seleccionat.
     *
     * @throws IOException Excepció a mostrar en cas que no es trobi el Layout
     * @author Víctor García - Creació
     */
    private void goToOrderDetails(Comanda t) throws IOException {
        setComandaActual(t);

        App.setRoot("comandesForm");
    }

    /**
     * Estableix la comanda seleccionada
     *
     * @param c Comanda
     * @author Pablo Morante - Creació/Implementació
     * @author Víctor García - Creació/Implementació
     */
    private void setComandaActual(Comanda c) {
        comandaActual = c;
    }

    /**
     * Mètode per saber quina comanda ha estat seleccionada
     *
     * @return Comanda
     * @author Pablo Morante - Creació/Implementació
     * @author Víctor García - Creació/Implementació
     */
    public static Comanda getComanda() {
        return comandaActual;
    }

    /**
     * Mostra l'apartat 'Clients' i al llistat que conté tots els registres de
     * la BD.
     *
     * @throws IOException Excepció a mostrar en cas que no es trobi el Layout
     * @author Txell Llanas - Creació
     */
    @FXML
    private void goToCustomers() throws IOException {
        App.setRoot("clients");
    }

    /**
     * Mostra l'apartat 'Productes' i al llistat que conté tots els registres de
     * la BD.
     *
     * @throws IOException Excepció a mostrar en cas que no es trobi el Layout
     * @author Txell Llanas - Creació
     */
    @FXML
    private void goToProducts() throws IOException {
        App.setRoot("productes");
    }

    /**
     * Mostra l'apartat 'Crèdits'. Indica la versió de l'aplicació i els seus
     * desenvolupadors.
     *
     * @throws IOException Excepció a mostrar en cas que no es trobi el Layout
     * @author Txell Llanas - Creació
     */
    @FXML
    private void goToAbout() throws IOException {
        App.setRoot("credits");
    }

    /**
     * Filtra les comandes entre dues dates
     *
     * @author Víctor García - Creació/Implementació
     */
    private void filteredTable() {
        ObservableList<Comanda> allItems = llistaObservableComanda;
        FilteredList<Comanda> filteredItems = new FilteredList<>(allItems);
        // bind predicate based on datepicker choices
        filteredItems.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            LocalDate minDate = datePickerFrom.getValue();
            LocalDate maxDate = datePickerTo.getValue();

            // get final values != null
            final LocalDate finalMin = minDate == null ? LocalDate.MIN : minDate;
            final LocalDate finalMax = maxDate == null ? LocalDate.MAX : maxDate;

            // values for openDate need to be in the interval [finalMin, finalMax]
            return ti -> !finalMin.isAfter(ti.getDataEntrega().toLocalDateTime().toLocalDate()) && !finalMax.isBefore(ti.getDataEntrega().toLocalDateTime().toLocalDate());
        },
                datePickerFrom.valueProperty(),
                datePickerTo.valueProperty()));

        ordersList.setItems(filteredItems);
    }

    /**
     * Neteja els filtres de cercar comanda entre dues dates
     *
     * @author Víctor García - Creació/Implementació
     * @param event
     */
    @FXML
    private void ClearHores(ActionEvent event) {
        datePickerFrom.setValue(null);
        datePickerTo.setValue(null);
    }
}
