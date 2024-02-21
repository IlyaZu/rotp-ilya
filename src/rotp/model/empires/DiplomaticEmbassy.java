/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2024 Ilya Zushinskiy
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.empires;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.AlliedWithEnemyIncident;
import rotp.model.incidents.AtWarWithAllyIncident;
import rotp.model.incidents.BreakAllianceIncident;
import rotp.model.incidents.BreakPactIncident;
import rotp.model.incidents.BreakTradeIncident;
import rotp.model.incidents.DeclareWarIncident;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.ErraticWarIncident;
import rotp.model.incidents.ExchangeTechnologyIncident;
import rotp.model.incidents.ExpansionIncident;
import rotp.model.incidents.MilitaryBuildupIncident;
import rotp.model.incidents.OathBreakerIncident;
import rotp.model.incidents.SignAllianceIncident;
import rotp.model.incidents.SignPactIncident;
import rotp.model.incidents.SignPeaceIncident;
import rotp.model.incidents.TrespassingIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.GNNAllianceBrokenNotice;
import rotp.ui.notifications.GNNAllianceFormedNotice;
import rotp.ui.notifications.GNNAllyAtWarNotification;
import rotp.util.Base;

public class DiplomaticEmbassy implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final float MAX_ADJ_POWER = 10;

    public static final int TIMER_SPY_WARNING = 0;
    public static final int TIMER_ATTACK_WARNING = 1;

    public static final int TECH_DELAY = 1;
    public static final int TRADE_DELAY = 10;
    public static final int PEACE_DELAY = 10;
    public static final int PACT_DELAY = 20;
    public static final int ALLIANCE_DELAY = 30;
    public static final int JOINT_WAR_DELAY = 20;
    public static final int UNALLY_DELAY = 30;
    public static final int MAX_REQUESTS_TURN = 4;

    private final EmpireView view;
    private final Map<String, DiplomaticIncident> incidents = new HashMap<>();
    private HashMap<String, List<String>> offeredTechs = new HashMap<>();
    private transient List<DiplomaticIncident> newIncidents = new ArrayList<>();

    private boolean contact = false;
    private int contactTurn = 0;
    private int treatyTurn = -1;
    private boolean warFooting = false;
    // using 'casus belli' as variable name since using that word means I made a smart AI
    private String casusBelli;
    private DiplomaticIncident casusBelliInc;
    private DiplomaticTreaty treaty;

    private final int[] timers = new int[20];
    private float relations;
    private int peaceDuration = 0; // obsolete - sunset at some point
    private int tradeTimer = 0;
    private int lastRequestedTradeLevel = 0;
    private int tradeRefusalCount = 0;
    private int techTimer = 0;
    private int peaceTimer = 0;
    private int pactTimer = 0;
    private int allianceTimer = 0;
    private int jointWarTimer = 0;
    private int diplomatGoneTimer = 0;
    private int warningLevel = 0;
    private boolean tradePraised = false;
    private int currentMaxRequests = MAX_REQUESTS_TURN;
    private int requestCount = 0;
    private int minimumPraiseLevel = 0;
    private int minimumWarnLevel = 0;
    private boolean threatened = false;

    public Empire empire()                               { return view.empire(); }
    public Empire owner()                                { return view.owner(); }
    public int treatyTurn()                            { return treatyTurn; }
    public DiplomaticTreaty treaty()                     { return treaty; }
    public String treatyStatus()                         { return treaty.status(owner()); }
    public Collection<DiplomaticIncident> allIncidents() { return incidents.values(); }
    public int requestCount()                            { return requestCount; }
    public float relations()                             { return relations; }
    public boolean contact()                             { return contact; }
    public boolean onWarFooting()                        { return warFooting; }
    public void beginWarPreparations(String cb, DiplomaticIncident inc) {
        // dont replace an existing casus belli unless the new one is worse
        if (casusBelliInc != null) {
            if (casusBelliInc.moreSevere(inc))
                return;
        }
        warFooting = true;
        casusBelli = cb;
        casusBelliInc = inc;
        view.spies().ignoreThreat();
        ignoreThreat();
    }
    public void endWarPreparations() {
        warFooting = false;
        casusBelli = null;
        casusBelliInc = null;
    }
    private void evaluateWarPreparations() {
        // we are assessing turn and about to enter diplomacy. Are our reasons
        // for going to war still relevant? If not, fuhgeddaboudit
        if (casusBelliInc != null) {
            if (!casusBelliInc.triggersWar())
                endWarPreparations();
            return;
        }
        
        if (casusBelli != null) {
            // re-evaluate hate and opportunity
            switch(casusBelli) {
                case DialogueManager.DECLARE_HATE_WAR: 
                    if (!view.owner().diplomatAI().wantToDeclareWarOfHate(view))
                        endWarPreparations();
                    break;
                case DialogueManager.DECLARE_OPPORTUNITY_WAR:
                    if (!view.owner().diplomatAI().wantToDeclareWarOfOpportunity(view))
                        endWarPreparations();
                    break;
            }
            return;
        }
    }
    public void contact(boolean b)                       { contact = b; }
    public List<DiplomaticIncident> newIncidents() {
        if (newIncidents == null)
            newIncidents = new ArrayList<>();
        return newIncidents;
    }
    private HashMap<String, List<String>> offeredTechs() {
        if (offeredTechs == null)
            offeredTechs = new HashMap<>();
        return offeredTechs;
    }
    public DiplomaticEmbassy(EmpireView v) {
        view = v;
        setNoTreaty();
        relations = baseRelations();
    }
    public final void setNoTreaty() {
        treaty = new TreatyNone(view.owner(), view.empire());
    }
    public float currentSpyIncidentSeverity() {
        float sev = 0;
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc.isSpying())
                sev += inc.currentSeverity();
        }
        return max(-50,sev);
    }
    public boolean hasCurrentSpyIncident() {
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc.isSpying() && (inc.turnOccurred() == galaxy().currentTurn()))
                return true;
        }
        return false;
    }
    public boolean hasCurrentAttackIncident() {
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc.isAttacking() && (inc.turnOccurred() == galaxy().currentTurn()))
                return true;
        }
        return false;
    }
    public void nextTurn(float prod) {
        peaceDuration--;
        treaty.nextTurn(empire());
    }
    public boolean war()                    { return treaty.isWar(); 	}
    public boolean noTreaty()               { return treaty.isNoTreaty(); }
    public boolean pact()                   { return treaty.isPact(); }
    public boolean alliance()               { return treaty.isAlliance(); }
    public boolean readyForTrade(int level) {
        // trade cooldown timer must be back to zero -AND-
        // new trade level must exceed last requested level by 25% * each consecutive refusal
        return (tradeTimer <= 0)
        && (level > (lastRequestedTradeLevel*(1+(tradeRefusalCount/4.0))));
    }
    public void resetTradeTimer(int level)  {
        if (empire().isPlayerControlled())
            tradeTimer = 1;
        else {
            tradeTimer = TRADE_DELAY;
            lastRequestedTradeLevel = level;
        }
    }
    public void tradeRefused()              { tradeRefusalCount++; }
    public void tradeAccepted()             { tradeRefusalCount = 0; }
    public boolean alreadyOfferedTrade()    { return tradeTimer == TRADE_DELAY; }
    public boolean readyForTech()           { return techTimer <= 0; }
    public void resetTechTimer()            { techTimer = TECH_DELAY; }
    public boolean alreadyOfferedTech()     { return techTimer == TECH_DELAY; }
    public boolean readyForPeace()          { return peaceTimer <= 0; }
    public void resetPeaceTimer()           { resetPeaceTimer(1); }
    public void resetPeaceTimer(int mult)   { peaceTimer = mult*PEACE_DELAY; }
    public boolean alreadyOfferedPeace()    { return peaceTimer == PEACE_DELAY; }
    public boolean readyForPact()           { return pactTimer <= 0; }
    public void resetPactTimer()            { pactTimer = PACT_DELAY; }
    public boolean alreadyOfferedPact()     { return pactTimer == PACT_DELAY; }
    public boolean readyForAlliance()       { return allianceTimer <= 0; }
    public void resetAllianceTimer()        { allianceTimer = ALLIANCE_DELAY; }
    public boolean alreadyOfferedAlliance() { return allianceTimer == ALLIANCE_DELAY; }
    public boolean readyForJointWar()       { return jointWarTimer <= 0; }
    public void resetJointWarTimer()        { jointWarTimer = empire().isPlayerControlled() ? JOINT_WAR_DELAY : 1; }
    public boolean alreadyOfferedJointWar() { return jointWarTimer == JOINT_WAR_DELAY; }
    public int minimumPraiseLevel()         { 
        // raise threshold for praise when at war
        if (war())
            return max(50, minimumPraiseLevel);
        else
            return max(10, minimumPraiseLevel); 
    }
    public int minimumWarnLevel()           { return max(10, minimumWarnLevel); }
    public void praiseSent()                { minimumPraiseLevel = minimumPraiseLevel()+10;  }
    public void logWarning(DiplomaticIncident inc) { 
        minimumWarnLevel = minimumWarnLevel()+5;  
        int timerKey = inc.timerKey();
        if (timerKey >= 0) {
            int duration = inc.duration();
            timers[timerKey] += duration;
        }
    }
    public boolean timerIsActive(int timerKey) {
        return (timerKey >= 0) && (timers[timerKey] > 0);
    }
    public void resetTimer(int index) {
        if ((index <0) || (index >= timers.length))
            return;
        timers[index] = 0;
    }
    public void giveExpansionWarning()      { warningLevel = 1; }
    public boolean gaveExpansionWarning()   { return warningLevel > 0; }
    public void noteRequest() {
        requestCount++;
    }
    public void heedThreat()          { threatened = true; }
    public void ignoreThreat()        { threatened = false; }
    public boolean threatened()       { return threatened; }
    
    public boolean tooManyRequests()        { return requestCount > currentMaxRequests; }
    public float otherRelations()          { return otherEmbassy().relations(); }
    public int contactAge()                 { return (galaxy().currentTurn() - contactTurn); }
    public DiplomaticEmbassy otherEmbassy() { return view.otherView().embassy(); }
    public boolean tradePraised()           { return tradePraised; }
    public void tradePraised(boolean b)     { tradePraised = b; }
    public void logTechExchangeRequest(Tech wantedTech, List<Tech> counterTechs) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            offeredTechs().put(wantedTech.id(), new ArrayList<>());

        List<String> list = offeredTechs().get(wantedTech.id());
        for (Tech t: counterTechs) {
            if (!list.contains(t.id()))
                list.add(t.id());
        }
    }
    public List<Tech> alreadyOfferedTechs(Tech wantedTech) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            return null;

        List<Tech> techs = new ArrayList<>();
        for (String s: offeredTechs().get(wantedTech.id()))
            techs.add(tech(s));

        return techs;
    }
    private void withdrawAmbassador(int turns) {
        diplomatGoneTimer = turns;
    }
    public void withdrawAmbassador() {
        int baseTurns = 2;
        if (empire().leader().isDiplomat())
            baseTurns /= 2;
        else if (empire().leader().isXenophobic())
            baseTurns *= 2;

        if (war())
            baseTurns *= 2;
        withdrawAmbassador(baseTurns+1);
    }
    public void assessTurn() {
        log(view+" Embassy: assess turn");
        evaluateWarPreparations();
        driftRelations();
        resetIncidents();

        // player refusals are remembered for the 
        // entire duration to avoid the AI spamming the player
        // AI  refusals are completely reset after each turn
        // to allow players to continue asking once each turn
        // if they want
        if (view.owner().isPlayerControlled()) {
            tradeTimer--;
            techTimer--;
            peaceTimer--;
            pactTimer--;
            allianceTimer--;
        }
        else {
            tradeTimer = 0;
            techTimer = 0;
            peaceTimer = 0;
            pactTimer = 0;
            allianceTimer = 0;

        }
        
        // decrement all generic timers down to 0
        for (int i=0;i<timers.length;i++) 
            timers[i] = max(0, timers[i]-1);
        
        // check if any threat timers have aged out
        if (!timerIsActive(TIMER_ATTACK_WARNING))
            ignoreThreat();
        if (!timerIsActive(TIMER_SPY_WARNING))
            view.spies().ignoreThreat();
        
        diplomatGoneTimer--;
        requestCount = 0;
        currentMaxRequests = min(currentMaxRequests+1, MAX_REQUESTS_TURN);
        minimumPraiseLevel = min(20,minimumPraiseLevel);
        minimumWarnLevel = min(20, minimumWarnLevel);
        minimumPraiseLevel = minimumPraiseLevel() - 1;
        minimumWarnLevel = minimumWarnLevel() - 1;
    }
    public void recallAmbassador()     { diplomatGoneTimer = Integer.MAX_VALUE; }
    public void openEmbassy()          { diplomatGoneTimer = 0; }
    public boolean diplomatGone()      { return diplomatGoneTimer > 0;  }
    public boolean wantWar()           { return otherEmbassy().relations() < -50; }
    public boolean isAlly()            { return alliance(); }
    public boolean alliedWithEnemy() {
        List<Empire> myEnemies = owner().warEnemies();
        List<Empire> hisAllies = empire().allies();
        for (Empire cv1 : myEnemies) {
            for (Empire cv2 : hisAllies) {
                if (cv1 == cv2)
                    return true;
            }
        }
        return false;
    }
    public boolean canAttackWithoutPenalty() { return anyWar() || noTreaty(); }
    public boolean canAttackWithoutPenalty(StarSystem s) {
        if (anyWar() || noTreaty())
            return true;
        if (pact())
            return (s.hasColonyForEmpire(owner()) || s.hasColonyForEmpire(empire()));
        if (alliance())
            return false;
        return !peaceTreatyInEffect();
    }
    public boolean peaceTreatyInEffect()   { return treaty.isPeace() || (peaceDuration > 0); }
    private void setTreaty(DiplomaticTreaty tr) {
        treaty = tr;
        otherEmbassy().treaty = tr;
        view.setSuggestedAllocations();
        view.otherView().setSuggestedAllocations();
    }
    public boolean isFriend()    { return pact() || alliance(); }
    public boolean isEnemy()     { return anyWar() || onWarFooting(); }
    public boolean anyWar()      { return war(); }
    public boolean atPeace()     { return peaceTreatyInEffect(); }

    public DiplomaticIncident exchangeTechnology(Tech offeredTech, Tech requestedTech) {
        // civ() is the requestor, and will be learning the requested tech
        // owner() is the requestee, who will be learning the counter-offered tech
        owner().tech().acquireTechThroughTrade(offeredTech.id, empire().id);
        empire().tech().acquireTechThroughTrade(requestedTech.id, owner().id);

        view.spies().noteTradedTech(requestedTech);
        view.otherView().spies().noteTradedTech(offeredTech);
        DiplomaticIncident inc = ExchangeTechnologyIncident.create(owner(), empire(), offeredTech, requestedTech);
        addIncident(inc);
        otherEmbassy().addIncident(ExchangeTechnologyIncident.create(empire(), owner(), requestedTech, offeredTech));
        return inc;
    }
    public void establishTradeTreaty(int level) {
        view.embassy().tradePraised(false);
        view.trade().startRoute(level);
    }
    public DiplomaticIncident declareJointWar(Empire requestor) {
        // when we are declaring a war as a result of a joint war request, ignore
        // any existing casus belli. This ensures that a DeclareWarIncident is returned 
        // instead of some existing casus belli incident. This ensures that [other...]
        // tags are replaced properly in the war announcement to the player
        casusBelli = null;
        casusBelliInc = null;
        return declareWar(requestor);
    }
    public DiplomaticIncident declareWar() {
        return declareWar(null);
    }
    public DiplomaticIncident declareWar(Empire requestor) {
        endTreaty();
        int oathBreakType = 0;
        if (alliance())
            oathBreakType = 1;
        else if (pact())
            oathBreakType = 2;

        view.trade().stopRoute();

        // if we're not at war yet, start it and inform player if he is involved
        if (!anyWar()) {
            setTreaty(new TreatyWar(view.owner(), view.empire()));
            if (view.empire().isPlayerControlled()) {
                if ((casusBelli == null) || casusBelli.isEmpty())
                    DiplomaticNotification.createAndNotify(view, DialogueManager.DECLARE_HATE_WAR);
                else
                    DiplomaticNotification.createAndNotify(view, casusBelli);
            }
        }

        resetTimer(TIMER_SPY_WARNING);
        resetTimer(TIMER_ATTACK_WARNING);
        resetPeaceTimer(3);
        withdrawAmbassador();
        otherEmbassy().withdrawAmbassador();

        // add war-causing incident to embassy
        DiplomaticIncident inc = casusBelliInc;
        if (inc == null) {
            if (casusBelli == null)
                inc = DeclareWarIncident.create(owner(), empire());
            else {
                switch(casusBelli) {
                    case DialogueManager.DECLARE_ERRATIC_WAR :
                        inc = ErraticWarIncident.create(owner(), empire()); break;
                    case DialogueManager.DECLARE_HATE_WAR:
                    default:
                        inc = DeclareWarIncident.create(owner(), empire());
                        oathBreakType = 0;
                        break;
                }
            }
        }
        otherEmbassy().addIncident(inc);

        // if oath broken, then create that incident as well
        switch(oathBreakType) {
            case 1:
            	GNNAllianceBrokenNotice.create(owner(), empire());
                OathBreakerIncident.alertBrokenAlliance(owner(),empire(),requestor,false); break;
            case 2: OathBreakerIncident.alertBrokenPact(owner(),empire(),requestor,false); break;
        }
        
        // if the player is one of our allies, let him know
        for (Empire ally : owner().allies()) {
            if (ally.isPlayerControlled())
                GNNAllyAtWarNotification.create(owner(), empire());
        }
        // if the player is one of our enemy's allies, let him know
        for (Empire ally : empire().allies()) {
            if (ally.isPlayerControlled())
                GNNAllyAtWarNotification.create(empire(), owner());
        }

        return inc;
    }
    public DiplomaticIncident breakTrade() {
        view.trade().stopRoute();
        DiplomaticIncident inc = BreakTradeIncident.create(owner(), empire());
        otherEmbassy().addIncident(inc);
        return inc;
    }
    public DiplomaticIncident signPeace() {
        beginTreaty();
        int duration = roll(10,15);
        endWarPreparations();
        otherEmbassy().endWarPreparations();
        beginPeace(duration);
        otherEmbassy().beginPeace(duration);
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        DiplomaticIncident inc = SignPeaceIncident.create(owner(), empire(), duration);
        addIncident(inc);
        otherEmbassy().addIncident(SignPeaceIncident.create(empire(), owner(), duration));
        return inc;
    }
    public DiplomaticIncident signPact() {
        beginTreaty();
        endWarPreparations();
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        setTreaty(new TreatyPact(view.owner(), view.empire()));
        DiplomaticIncident inc = SignPactIncident.create(owner(), empire());
        addIncident(inc);
        otherEmbassy().addIncident(SignPactIncident.create(empire(), owner()));
        return inc;
    }
    public void reopenEmbassy() {
        diplomatGoneTimer = 0;
    }
    public void closeEmbassy() {
        withdrawAmbassador(Integer.MAX_VALUE);
    }
    public DiplomaticIncident breakPact() { return breakPact(false); }
    public DiplomaticIncident breakPact(boolean caughtSpying) {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakPactIncident.create(owner(), empire(), caughtSpying);
        otherEmbassy().addIncident(inc);
        OathBreakerIncident.alertBrokenPact(owner(),empire(), caughtSpying);
        return inc;
    }
    public DiplomaticIncident signAlliance() {
        beginTreaty();
        endWarPreparations();
        setTreaty(new TreatyAlliance(view.owner(), view.empire()));
        owner().setRecalcDistances();
        empire().setRecalcDistances();
        owner().shareSystemInfoWithAlly(empire());
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        DiplomaticIncident inc = SignAllianceIncident.create(owner(), empire());
        addIncident(inc);
        otherEmbassy().addIncident(SignAllianceIncident.create(empire(), owner()));
        GNNAllianceFormedNotice.create(owner(), empire());
        return inc;
    }
    public DiplomaticIncident breakAlliance() { return breakAlliance(false); }
    public DiplomaticIncident breakAlliance(boolean caughtSpying) {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakAllianceIncident.create(owner(), empire(), caughtSpying);
        otherEmbassy().addIncident(inc);
        GNNAllianceBrokenNotice.create(owner(), empire());
        OathBreakerIncident.alertBrokenAlliance(owner(),empire(),caughtSpying);
        return inc;
    }
    public void setContact() {
        if (!contact()) {
            contactTurn = galaxy().currentTurn();
            contact(true);
        }
    }
    public void makeFirstContact() {
        log("First Contact: ", owner().name(), " & ", empire().name());
        setContact();
        if (empire().isPlayerControlled())
            DiplomaticNotification.create(view, owner().leader().dialogueContactType());
    }
    public void removeContact() {
        contact = false;
        resetTreaty();
        view.spies().beginHide();
        view.trade().stopRoute();
        if (otherEmbassy().contact)
            otherEmbassy().removeContact();
    }
    public void resetTreaty()   { setTreaty(new TreatyNone(view.owner(), view.empire())); }
    public void addIncident(DiplomaticIncident inc) {
        // add new incidents to current list
        // hash by incident key to filter out overlapping events
        String k = inc.key();
        log("addIncident key:"+k);
        DiplomaticIncident matchingEvent = incidents.get(k);
        log(view.toString(), ": Adding incident- ", inc.key(), ":", str(inc.currentSeverity()), ":", inc.toString());
        if (inc.moreSevere(matchingEvent)) {
            incidents.put(k,inc);
            updateRelations(inc);
            treaty.noticeIncident(inc);
        }
    }
    public DiplomaticIncident getIncidentWithKey(String key) {
        return incidents.get(key);
    }
    
    private void driftRelations() {
    	updateRelations((baseRelations()-relations)/100);
    }
    
    private void updateRelations(DiplomaticIncident incident) {
    	updateRelations(incident.currentSeverity());
    }
    
    private void updateRelations(float severity) {
    	severity = adjustSeverity(severity);
    	relations = bounds(-100, relations+severity, 100);
    }
    
    private float adjustSeverity(float severity) {
    	// Negative severity is treated the same with the relations range flipped.
    	float adjustedRelations = relations * Math.signum(severity);
    	
    	float modifier;
    	if (adjustedRelations < 0) {
    		// relations is negative at this point so this is an add.
    		modifier = 1 - adjustedRelations/50; // 1 to 3
    	} else {
    		modifier = 1 / (1 + adjustedRelations/25); // 1 to 1/5
    	}
    	
    	return severity * modifier;
    }
    
    private float baseRelations() {
    	return owner().baseRelations(empire());
    }

    private void resetIncidents() {
        newIncidents().clear();
        clearForgottenIncidents();
        
        AtWarWithAllyIncident.create(view);
        AlliedWithEnemyIncident.create(view);
        TrespassingIncident.create(view);
        ExpansionIncident.create(view);
        MilitaryBuildupIncident.create(view);

        // make special list of incidents added in this turn
        for (DiplomaticIncident ev: incidents.values()) {
            if ((galaxy().currentTurn() - ev.turnOccurred()) < 1)
                newIncidents().add(ev);
        }
    }
    private void clearForgottenIncidents() {
        List<String> keys = new ArrayList<>(incidents.keySet());
        for (String key: keys) {
            DiplomaticIncident inc = incidents.get(key);
            if (inc.isForgotten()) {
                log("Forgetting: ", incidents.get(key).toString());
                incidents.remove(key);
            }
        }
    }
    private void beginTreaty() {
        treatyTurn = galaxy().currentTurn();
        otherEmbassy().treatyTurn = galaxy().currentTurn();
    }
    private void endTreaty() {
        treatyTurn = -1;
        otherEmbassy().treatyTurn = -1;
        resetPactTimer();
        resetAllianceTimer();
        otherEmbassy().resetPactTimer();
        otherEmbassy().resetAllianceTimer();
        owner().setRecalcDistances();
        empire().setRecalcDistances();
    }
    private void beginPeace(int duration) {
        treaty = new TreatyPeace(view.empire(), view.owner(), duration);
        view.setSuggestedAllocations();
    }
}