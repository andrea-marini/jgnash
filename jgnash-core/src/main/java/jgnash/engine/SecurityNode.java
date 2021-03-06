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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;

import jgnash.util.DateUtils;

/**
 * Security Node
 *
 * @author Craig Cavanaugh
 */
@Entity
public class SecurityNode extends CommodityNode {

    @ManyToOne
    private CurrencyNode reportedCurrency;

    /**
     * The currency that security values are reported in
     */
    @Enumerated(EnumType.STRING)
    private QuoteSource quoteSource = QuoteSource.NONE;

    /**
     * ISIN or CUSIP.  Used for OFX and quote downloads
     */
    private String isin;

    @JoinTable
    @OrderBy("date")    //applying a sort order prevents refresh issues
    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    private Set<SecurityHistoryNode> historyNodes = new HashSet<>();

    @JoinTable
    @OrderBy("date")    //applying a sort order prevents refresh issues
    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    private Set<SecurityHistoryEvent> securityHistoryEvents = new HashSet<>();

    private transient ReadWriteLock lock;

    private transient List<SecurityHistoryNode> sortedHistoryNodeCache = new ArrayList<>();

    public SecurityNode() {
        lock = new ReentrantReadWriteLock(true);
    }

    public SecurityNode(final CurrencyNode node) {
        this();
        setReportedCurrencyNode(node);
    }

    /**
     * Prefix is deferred to the reported currency
     *
     * @return prefix of the reported currency
     */
    @Override
    public String getPrefix() {
        return reportedCurrency.getPrefix();
    }

    @Override
    public void setPrefix(final String ignored) {
    }

    /**
     * Suffix is deferred to the reported currency
     *
     * @return suffix of the reported currency
     */
    @Override
    public String getSuffix() {
        return reportedCurrency.getSuffix();
    }

    @Override
    public void setSuffix(final String ignored) {
    }

    /**
     * Returns the quote download source
     *
     * @return quote download source
     */
    public QuoteSource getQuoteSource() {
        return quoteSource;
    }

    /**
     * Sets the quote download source
     *
     * @param source QuoteSource to use
     */
    public void setQuoteSource(final QuoteSource source) {
        quoteSource = source;
    }

    public String getISIN() {
        return isin;
    }

    public void setISIN(final String isin) {
        this.isin = isin;
    }

    /**
     * Set the CurrencyNode that security histories are reported in
     *
     * @param node reported CurrencyNode
     */
    public void setReportedCurrencyNode(final CurrencyNode node) {
        reportedCurrency = node;
    }

    /**
     * Returns the CurrencyNode that security histories are reported in
     *
     * @return reported CurrencyNode
     */
    public CurrencyNode getReportedCurrencyNode() {
        return reportedCurrency;
    }

    boolean addHistoryNode(final SecurityHistoryNode node) {

        lock.writeLock().lock();

        try {
            sortedHistoryNodeCache.add(node);
            Collections.sort(sortedHistoryNodeCache);

            return historyNodes.add(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean removeHistoryNode(final LocalDate date) {

        boolean result = false;

        SecurityHistoryNode nodeToRemove = null;

        lock.writeLock().lock();

        try {
            for (final SecurityHistoryNode node : historyNodes) {
                if (node.getLocalDate().compareTo(date) == 0) {
                    nodeToRemove = node;
                    break;
                }
            }

            // Remove outside the iterator
            if (nodeToRemove != null) {
                sortedHistoryNodeCache.remove(nodeToRemove);
                result = historyNodes.remove(nodeToRemove);
            }
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }

    boolean addSecurityHistoryEvent(final SecurityHistoryEvent securityHistoryEvent) {
        boolean result = false;

        lock.writeLock().lock();

        try {
            result = securityHistoryEvents.add(securityHistoryEvent);
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }

    boolean removeSecurityHistoryEvent(final SecurityHistoryEvent securityHistoryEvent) {
        boolean result = false;

        lock.writeLock().lock();

        try {
            // Use equality check for remove
            for (final SecurityHistoryEvent historyEvent : securityHistoryEvents) {
                if (historyEvent.equals(securityHistoryEvent)) {
                    result = securityHistoryEvents.remove(historyEvent);
                    break;  // break to prevent concurrent modification error
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }


    /**
     * Returns <tt>true</tt> if this SecurityNode contains the specified element.
     *
     * @param date LocalDate whose presence in this SecurityNode is to be tested
     * @return <tt>true</tt> if this SecurityNode contains a SecurityHistoryNode with the specified date
     */
    public boolean contains(final LocalDate date) {
        boolean result = false;

        lock.readLock().lock();

        try {
            for (final SecurityHistoryNode node : historyNodes) {
                if (node.getLocalDate().compareTo(date) == 0) {
                    result = true;
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    /**
     * Gets the SecurityHistoryNodes for this security.  At time of retrieval the adjusted price of the
     * SecurityHistoryNodes will be updated to reflect any spits or reverse splits
     *
     * @return Returns a shallow copy of the history nodes to protect against modification
     * @see SecurityHistoryNode#getAdjustedPrice()
     */
    public List<SecurityHistoryNode> getHistoryNodes() {

        lock.writeLock().lock();

        try {
            final List<SecurityHistoryEvent> splits = getSplitEvents();

            if (splits.size() > 0) {
                BigDecimal scalar = BigDecimal.ONE;

                final ListIterator<SecurityHistoryEvent> historyEventIterator = splits.listIterator(splits.size());

                LocalDate eventDate = historyEventIterator.previous().getDate();
                historyEventIterator.next();    // reset back to the tail

                // work backwards
                for (int i = sortedHistoryNodeCache.size() - 1; i >= 0; i--) {
                    if (DateUtils.after(eventDate, sortedHistoryNodeCache.get(i).getLocalDate())) {
                        if (historyEventIterator.hasPrevious()) {
                            final SecurityHistoryEvent historyEvent = historyEventIterator.previous();
                            eventDate = historyEvent.getDate();
                            scalar = scalar.divide(historyEvent.getValue(), MathConstants.mathContext);
                        }
                    }

                    sortedHistoryNodeCache.get(i).setAdjustmentMultiplier(scalar);
                }
            }

            return Collections.unmodifiableList(sortedHistoryNodeCache);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Convience function to return the upper and lower date bounds.  If
     *
     * @return an array of LocalDate with {@code [0]} being the lower bound and {@code [1]} being the upper bound
     */
    public Optional<LocalDate[]> getLocalDateBounds() {
        lock.readLock().lock();

        try {
            if (sortedHistoryNodeCache.size() > 1) {
                return Optional.of(new LocalDate[]{
                        sortedHistoryNodeCache.get(0).getLocalDate(),
                        sortedHistoryNodeCache.get(sortedHistoryNodeCache.size() - 1).getLocalDate()
                });
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the history node events split into groups by historical splits or reverse splits
     *
     * @return a List of Lists of SecurityHistoryNodes
     */
    public List<List<SecurityHistoryNode>> getHistoryNodeGroupsBySplits() {

        final List<List<SecurityHistoryNode>> groups = new ArrayList<>();
        final List<SecurityHistoryEvent> splitEvents = getSplitEvents();

        if (splitEvents.size() == 0) {
            groups.add(getHistoryNodes());
        } else {    // count should be split events + 1 when complete

            // Create a defensive copy that has the adjustment multiplier set
            final List<SecurityHistoryNode> securityHistoryNodes = getHistoryNodes();
            final ListIterator<SecurityHistoryEvent> historyEventIterator = splitEvents.listIterator();

            LocalDate eventDate = historyEventIterator.next().getDate();

            List<SecurityHistoryNode> group = new ArrayList<>();

            for (int i = 0; i < securityHistoryNodes.size(); i++) {
                if (eventDate == null || securityHistoryNodes.get(i).getLocalDate().isBefore(eventDate)) {
                    group.add(securityHistoryNodes.get(i));
                } else {
                    groups.add(group);                              // save the current group
                    group = new ArrayList<>();                      // start a new group
                    group.add(securityHistoryNodes.get(i - 1));      // create continuity with the previous group
                    group.add(securityHistoryNodes.get(i));          // add the current node

                    if (historyEventIterator.hasNext()) {
                        eventDate = historyEventIterator.next().getDate();
                    } else {
                        eventDate = null;
                    }
                }
            }
            groups.add(group);  // add last group
        }

        return groups;
    }

    /**
     * Get an unmodifiable copy of the SecurityHistoryEvents for this security
     *
     * @return returns a shallow copy of the SecurityHistoryEvents to protect against modification
     */
    public Set<SecurityHistoryEvent> getHistoryEvents() {
        return Collections.unmodifiableSet(securityHistoryEvents);
    }

    public List<SecurityHistoryEvent> getSplitEvents() {
        lock.readLock().lock();

        try {
            final List<SecurityHistoryEvent> splits = securityHistoryEvents.stream().filter(historyEvent
                    -> historyEvent.getType() == SecurityHistoryEventType.SPLIT).collect(Collectors.toList());

            Collections.sort(splits);

            return splits;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the {@code SecurityHistoryNode} with the matching date
     *
     * @param date Date to match
     * @return {@code Optional} contain a matching node
     */
    public Optional<SecurityHistoryNode> getHistoryNode(final LocalDate date) {
        lock.readLock().lock();

        try {
            SecurityHistoryNode hNode = null;

            // Work backwards through the list as the newest date is requested the most
            for (int i = sortedHistoryNodeCache.size() - 1; i >= 0; i--) {
                final SecurityHistoryNode node = sortedHistoryNodeCache.get(i);

                if (date.compareTo(node.getLocalDate()) == 0) {
                    hNode = node;
                    break;
                }
            }

            return Optional.ofNullable(hNode);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the {@code SecurityHistoryNode} with the closet matching date without exceeding the request date
     *
     * @param date {@code Date} to match
     * @return {@code Optional} containing a {@code SecurityHistoryNode} if a match is found
     */
    public Optional<SecurityHistoryNode> getClosestHistoryNode(final LocalDate date) {
        final long epochDay = date.toEpochDay();

        lock.readLock().lock();

        try {
            SecurityHistoryNode hNode = null;

            // Work backwards through the list as the newest date is requested the most
            for (int i = sortedHistoryNodeCache.size() - 1; i >= 0; i--) {
                final SecurityHistoryNode node = sortedHistoryNodeCache.get(i);

                if (node.getLocalDate().toEpochDay() <= epochDay) {
                    hNode = node;
                    break;
                }
            }

            return Optional.ofNullable(hNode);
        } finally {
            lock.readLock().unlock();
        }
    }

    private BigDecimal getMarketPrice(final LocalDate date) {
        BigDecimal marketPrice = BigDecimal.ZERO;

        final Optional<SecurityHistoryNode> optional = getClosestHistoryNode(date);

        if (optional.isPresent()) {
            marketPrice = optional.get().getPrice();
        }

        return marketPrice;
    }

    /**
     * Returns the latest market price exchanged to the specified currency
     *
     * @param date date to find closest matching rate without exceeding
     * @param node currency to exchange to
     * @return latest market price
     */
    public BigDecimal getMarketPrice(final LocalDate date, final CurrencyNode node) {
        return getMarketPrice(date).multiply(getReportedCurrencyNode().getExchangeRate(node));
    }

    /**
     * Return a clone of this security node.  Security history is not cloned
     *
     * @return clone of this SecurityNode with history nodes
     */
    @Override
    public Object clone() throws CloneNotSupportedException {

        lock.readLock().lock();

        try {
            SecurityNode node = (SecurityNode) super.clone();
            node.historyNodes = new HashSet<>();
            node.postLoad();

            return node;
        } finally {
            lock.readLock().unlock();
        }
    }

    protected Object readResolve() {
        postLoad();
        return this;
    }

    @PostLoad
    private void postLoad() {
        lock = new ReentrantReadWriteLock(true);

        // load the cache list
        sortedHistoryNodeCache = new ArrayList<>(historyNodes);
        Collections.sort(sortedHistoryNodeCache);   // JPA will be naturally sorted, but XML files will not
    }
}
