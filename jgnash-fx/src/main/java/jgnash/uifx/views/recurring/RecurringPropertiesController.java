/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.views.recurring;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.recurring.Reminder;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.views.register.TransactionDialog;
import jgnash.util.DateUtils;

/**
 * Controller for creating and modifying a reminder
 *
 * @author Craig Cavanaugh
 */
public class RecurringPropertiesController {

    private static final int CURRENT = 0;

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private TabPane tabs;

    @FXML
    private CheckBox enabledCheckBox;

    @FXML
    private TextField lastOccurrenceTextField;

    @FXML
    private IntegerTextField daysPastDueTextField;

    @FXML
    private CheckBox autoEnterCheckBox;

    @FXML
    private IntegerTextField daysBeforeTextField;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private TextField payeeTextField;

    @FXML
    private TextArea notesTextArea;

    private Transaction transaction;

    private final DateTimeFormatter dateFormatter = DateUtils.getShortDateTimeFormat();

    @FXML
    private void initialize() {
        daysBeforeTextField.disableProperty().bind(autoEnterCheckBox.selectedProperty().not());
    }

    void showReminder(final Reminder reminder) {
        final Account a = reminder.getAccount();

        if (a != null) {
            accountComboBox.setValue(a);
        } else {
            System.err.println("did not find account");
        }

        transaction = reminder.getTransaction();

        if (transaction != null) {
            payeeTextField.setText(transaction.getPayee());
        }

        descriptionTextField.setText(reminder.getDescription());
        enabledCheckBox.setSelected(reminder.isEnabled());
        startDatePicker.setDate(reminder.getStartDate());
        notesTextArea.setText(reminder.getNotes());

        //TODO freq tab

        final LocalDate lastOccurrence = DateUtils.asLocalDate(reminder.getLastDate());

        lastOccurrenceTextField.setText(dateFormatter.format(lastOccurrence));
        autoEnterCheckBox.setSelected(reminder.isAutoCreate());
        daysBeforeTextField.setInteger(reminder.getDaysAdvance());

        final LocalDate nextDate = DateUtils.asLocalDate(reminder.getIterator().next());

        final Period betweenDates = Period.between(nextDate, LocalDate.now());
        if (betweenDates.getDays() > 0) {
            daysPastDueTextField.setInteger(betweenDates.getDays());
        } else {
            daysPastDueTextField.setInteger(CURRENT);
        }
    }

    @FXML
    private void okAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleDeleteTransaction() {
        transaction = null;
        payeeTextField.setText(null);
    }

    @FXML
    private void handleEditTransaction() {
        final Optional<Transaction> optional = TransactionDialog.showAndWait(accountComboBox.getValue(), transaction);

        if (optional.isPresent()) {
            transaction = optional.get();
            payeeTextField.setText(transaction.getPayee());
        }
    }
}