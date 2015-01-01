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
package jgnash.uifx.views.register;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;

import javafx.scene.control.TableCell;

import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;

/**
 * {@code TableCell} for rendering investment transaction quantities
 *
 * @author Craig Cavanaugh
 */
class InvestmentTransactionQuantityTableCell extends TableCell<Transaction, BigDecimal> {

    public InvestmentTransactionQuantityTableCell() {

        // Right align
        setStyle("-fx-alignment: center-right;");
    }

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null && getTableRow() != null) {

            Transaction transaction = (Transaction) getTableRow().getItem();

            if (transaction instanceof InvestmentTransaction) {

                final NumberFormat numberFormat = CommodityFormat.getShortNumberFormat(((InvestmentTransaction) transaction).getSecurityNode());

                setText(numberFormat.format(amount));

                // Not empty and amount is not null, but tableRow can be null... JavaFx Bug?
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    final boolean future = ((Transaction) getTableRow().getItem()).getDate().after(new Date());
                    final boolean negative = amount.signum() < 0;

                    // Set font style
                    if (future && negative) {
                        setId("italic-negative-label");
                    } else if (future) {
                        setId("italic-label");
                    } else if (negative) {
                        setId("normal-negative-label");
                    } else {
                        setId("normal-label");
                    }
                }
            } else {
                setText(null);
            }

        } else {
            setText(null);
        }
    }
}
