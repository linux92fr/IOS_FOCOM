package com.example.ui.i18n

import com.example.data.model.FocomDelegate
import com.example.data.model.FocomNews
import com.example.data.model.FocomPublication
import com.example.data.model.FocomRight

fun AppLanguage.localizeDelegate(delegate: FocomDelegate): FocomDelegate {
    if (this == AppLanguage.FR) return delegate
    val translatedRole = when (delegate.id) {
        "ds_1" -> "General Secretary of FOCOM UES ILIAD Union - Full CSE Member, Union Delegate"
        "ds_2" -> "Works Supervisor - Union Delegate, Full CSE Member"
        "ds_3" -> "TMF (formerly TFO) - Union Delegate"
        "ds_4" -> "TMF (formerly TFO) - Full CSE Member, Union Delegate, CSSCT Member"
        "ds_5" -> "CIR (formerly VPI) - Union Representative"
        "rs_1" -> "TMRE (formerly PDEM) - Union Delegate, Full CSE Member, Site Administrator"
        else -> when {
            delegate.role.equals("Déléguée Syndicale Centrale", ignoreCase = true) -> "Central Union Delegate"
            delegate.role.equals("Délégué Syndical", ignoreCase = true) -> "Union Delegate"
            delegate.role.equals("Représentant Syndical", ignoreCase = true) -> "Union Representative"
            else -> delegate.role
        }
    }
    return delegate.copy(role = translatedRole)
}

fun AppLanguage.localizeRight(right: FocomRight): FocomRight {
    if (this == AppLanguage.FR) return right
    return englishRightsById[right.id]?.let { translated ->
        right.copy(
            title = translated.title,
            summary = translated.summary,
            detailMarkdown = translated.detailMarkdown,
            focomAdvice = translated.focomAdvice
        )
    } ?: right
}

fun AppLanguage.localizeNews(news: FocomNews): FocomNews {
    if (this == AppLanguage.FR) return news
    return englishNewsById[news.id]?.let { translated ->
        news.copy(
            title = translated.title,
            description = translated.description,
            content = translated.content,
            date = translated.date
        )
    } ?: news
}

fun AppLanguage.localizePublication(pub: FocomPublication): FocomPublication {
    if (this == AppLanguage.FR) return pub
    return englishPublicationsById[pub.id]?.let { translated ->
        pub.copy(
            title = translated.title,
            date = translated.date,
            excerpt = translated.excerpt.ifBlank { pub.excerpt },
            content = translated.content.ifBlank { pub.content }
        )
    } ?: pub
}

private val englishRightsById: Map<String, FocomRight> = mapOf(
    "right_1" to FocomRight(
        id = "right_1",
        title = "Remote Work & Group Charter",
        category = "Télétravail",
        summary = "Which roles are eligible and what allowances are provided under the agreement?",
        detailMarkdown = """Remote work within UES Iliad is governed by an amendment to the company collective agreement.

### Eligibility:
All permanent (CDI) and fixed-term (CDD) employees (excluding first-year apprentices) whose role is compatible are entitled to up to 2 remote workdays per week (sometimes 3 depending on the entity).

### Company support:
- Provision of a laptop, a 24-inch external monitor, a headset, and keyboard/mouse.
- Flat monthly remote-work allowance of €15 for 2 remote days per week.

### Your rights and obligations:
Your working hours remain strictly identical to on-site schedules. No intrusive presence monitoring or continuous audio/video recording is allowed. You retain the legal right not to reply to emails outside defined working hours.""",
        focomAdvice = "FOCOM is pushing to raise the allowance to €30/month to offset rising electricity and heating costs when working from home."
    ),
    "right_2" to FocomRight(
        id = "right_2",
        title = "Collective Seniority Leave",
        category = "Congés",
        summary = "See how your additional paid leave days for seniority are calculated.",
        detailMarkdown = """Under the French National Telecommunications Collective Agreement (CCNT Télécoms) applicable to UES Iliad, seniority grants additional paid leave days.

### Seniority leave days:
- **From 1 to under 5 years of seniority:** no extra day.
- **From 5 to under 10 years of seniority:** 1 extra paid leave day per year.
- **From 10 to under 15 years of seniority:** 2 extra paid leave days per year.
- **15 years or more of seniority:** 3 extra paid leave days per year.

### Special family-event leave:
- Marriage or civil partnership (PACS): 5 days off.
- Birth or adoption: 3 days off.
- Child's marriage: 1 day off.
- Death of a close relative (parent, child, spouse): 3 to 7 days of paid leave.""",
        focomAdvice = "Some Group entities try to bundle these days unfavorably. If you notice a missing day on your payslip (CP/RTT balance), alert FOCOM immediately."
    ),
    "right_3" to FocomRight(
        id = "right_3",
        title = "Health Insurance & Disability Cover",
        category = "Santé",
        summary = "Hospitalization, optical, dental coverage, and salary-continuation benefits.",
        detailMarkdown = """Iliad's corporate health plan is mandatory for all employees unless a valid exemption applies (partner coverage, short fixed-term contract).

### Coverage levels:
- **GP and specialist visits:** Covered at 100% to 150% of the Social Security base rate.
- **Optical:** Generous packages renewable every 2 years for frames and complex lenses (up to €350 depending on options).
- **Dental:** High reimbursement rates for crowns and routine dental care.

### Disability & life insurance (Prévoyance):
Guarantees 100% net salary continuation (topping up Social Security) for up to 90 days during continuous sick leave.""",
        focomAdvice = "FOCOM sits on the supervisory board of the Group health plan. We fight hard every year to block increases in employee contributions."
    ),
    "right_4" to FocomRight(
        id = "right_4",
        title = "Profit-Sharing & Incentive Schemes",
        category = "Rémunération",
        summary = "Calculation formulas, early release rules, and employer matching contributions.",
        detailMarkdown = """Value sharing at Iliad relies on two schemes: statutory profit-sharing (participation) and company incentives (intéressement).

### Company incentive (Intéressement):
Based on achieving operational targets set per entity (e.g., support response rates at Centrapel, fiber rollout targets at Free Réseau).

### Statutory profit-sharing (Participation):
Calculated directly from the consolidated net profit of the Iliad Group in France.

### Employer matching (Abondement):
Depending on your company agreement, investing your bonus in a savings plan (PEE/PERCO) instead of taking immediate cash can earn an employer top-up of 10% to 15%.""",
        focomAdvice = "We demand a fairer, solidarity-based incentive formula that does not penalize employees who were on sick leave or maternity leave during the year."
    ),
    "right_5" to FocomRight(
        id = "right_5",
        title = "Right to Strike (Survival FAQ)",
        category = "Législation",
        summary = "Can I strike alone? Is prior notice required? How do pay deductions work?",
        detailMarkdown = """The right to strike is a fundamental constitutional right in France (Article 7 of the 1946 Constitution Preamble).

### 1. Can I strike completely on my own?
In the private sector (like UES Iliad), you cannot strike alone unless you are joining a national call to strike or you are the sole employee at your site stopping work as part of a national union-backed movement like FOCOM. Otherwise, at least two employees must participate.

### 2. Mandatory prior notice:
Unlike the public sector, no prior notice is required in the private sector! You may stop work at any time provided the employer is informed of your professional demands when work stops.

### 3. Pay deduction:
Management may only deduct wages strictly proportional to the strike duration. Any excessive deduction or disciplinary sanction is unlawful.""",
        focomAdvice = "FOCOM advises always informing your local delegates before taking collective action so we can ensure full legal protection for your movement."
    ),
    "right_6" to FocomRight(
        id = "right_6",
        title = "Right to Alert & Right of Withdrawal",
        category = "Sécurité",
        summary = "What is the right of withdrawal? Steps to follow in case of burnout or harassment.",
        detailMarkdown = """Every worker has the right to step away from a work situation presenting a grave and imminent danger to their life or health.

### Right of Withdrawal (Art. L. 4131-1 et seq.):
- **Condition:** You must have reasonable grounds to believe the situation poses a grave and immediate danger (e.g., live faulty electrical equipment, assault, extreme cold).
- **Protection:** No wage deduction or sanction may be imposed. Management cannot order you back until the workstation is made safe.

### What to do in case of Burnout or Harassment?
A formal right of alert can be triggered by an FOCOM CSE representative whenever a physical or psychosocial risk (harassment, abusive managerial pressure) is identified.""",
        focomAdvice = "If you feel endangered or at risk of psychological collapse, do not wait! Exercise your right of withdrawal and contact an FOCOM delegate immediately."
    )
)

private val englishNewsById: Map<String, FocomNews> = mapOf(
    "news_nao_2026" to FocomNews(
        id = "news_nao_2026",
        title = "NAO 2026: FO will not sign this insufficient agreement",
        description = "After four meetings, FO denounces a capped 1.6% budget and a lack of genuine negotiations.",
        content = """FO COM POSITION
Management refuses to negotiate: FO will not sign this insufficient agreement.

After four meetings of sham negotiations, Management presented an overall envelope limited to 1.6%. For FO COM, these proposals fail to address inflation or employees' legitimate expectations.

NAO 2026 MEETINGS
- 15/04/2026: Opening talks and initial guidelines.
- 29/04/2026: Unions presented wage and social demands.
- 05/05/2026: Management kept a rigid cap despite demands.
- 13/05/2026: Final offer; FO denounces the lack of genuine bargaining.

BUDGETS PROPOSED BY MANAGEMENT
- UES ILIAD: 1.6% (1.2% pay reviews, 0.4% social measures).
- FREE RÉSEAU / ROF: 1.7% (1.2% pay reviews, 0.5% social measures).
- FREE MOBILE: 1.2% pay reviews, zero dedicated social measures.
- ILIAD / ASSUNET / FREE SAS: 1.2% pay reviews, zero dedicated social measures.

WAGE MEASURES
- Union demand: 2.2% to 2.9% global envelope.
- Management: Only 1.6% for UES ILIAD.
- Union demand: Collective increase for all employees.
- Management: 1% limited to packages of €30,000 or less.
- Automatic raise after 3 years without progression: rejected.
- Retroactive effect to January 1, 2026: rejected.
- Entry salary increase: +€240/year only.

WHY FO WILL NOT SIGN
1. A 1.6% global budget is insufficient given inflation and Group profits.
2. The 1% collective increase is restricted to packages <= €30,000, excluding most employees.
3. No retroactivity to January 1, 2026: months of lost purchasing power.
4. Restrictive €50 fuel bonus excluding most workers.
5. Zero real progress on seniority, working time, remote work, gender equality, or leave.
6. No real negotiation: Management imposed unilateral terms.

FO CONCLUSION
FO refuses to endorse this charade and will not sign. FO COM remains mobilized to defend purchasing power, social dialogue, and fair measures for all employees.""",
        date = "May 13, 2026",
        category = "NAO",
        isHotState = true
    ),
    "news_1" to FocomNews(
        id = "news_1",
        title = "2026 Annual Mandatory Negotiations (NAO): FOCOM's Demands",
        description = "FOCOM defends your purchasing power against inflation with concrete proposals.",
        content = """The 2026 Annual Mandatory Negotiations (NAO) for UES Iliad have started.

FOCOM stands up for all employees (Free Réseau, Free Mobile, Iliad, Free SAS, Assunet, ROF) with strong demands:
- A 4.5% general pay rise for all salaries under €3,500 gross.
- Meal vouchers raised to €10 (60% employer-funded).
- A €1,500 Value Sharing Bonus (PPV) to support lower salaries.
- Harmonization of minimum pay scales across Group entities.

Your mobilization gives us strength at the negotiating table. Let's stay united!""",
        date = "May 22, 2026",
        category = "NAO",
        isHotState = false
    ),
    "news_2" to FocomNews(
        id = "news_2",
        title = "CSE Elections: Historic Representativeness Victory for FOCOM!",
        description = "Thanks to your votes, FOCOM strengthens its presence across employee bodies.",
        content = """The Social and Economic Committee (CSE) election results are in!

Thanks to your continued trust, FOCOM achieved a historic +6% gain across UES Iliad, winning new elected seats at Free Réseau, Free Mobile, and Free SAS.

Your FOCOM representatives pledge to:
- Closely monitor company financial health and job security.
- Improve CSE social benefits (holiday vouchers, tickets, year-end gifts).
- Stay present in the field daily to answer questions and resolve disputes.

Thank you for your support—the fight for stronger rights continues!""",
        date = "May 15, 2026",
        category = "CSE",
        isHotState = false
    ),
    "news_3" to FocomNews(
        id = "news_3",
        title = "Remote Work: Toward a More Flexible & Better Compensated Agreement",
        description = "FOCOM is negotiating to expand remote work to 3 days per week.",
        content = """The current remote work charter at Iliad is expiring. FOCOM requested new talks for a stronger company agreement, notably for Free Mobile and Assunet.

Our core proposals:
- Expand remote work up to 3 days per week for all eligible roles.
- Introduce a flat €30/month allowance for electricity, heating, and internet.
- Provide a €120 equipment allowance for ergonomic gear (chair, monitor, footrest).
- Strictly enforce the right to disconnect outside working hours.

Management's response is expected shortly. We will keep you updated!""",
        date = "May 8, 2026",
        category = "Accords",
        isHotState = false
    ),
    "news_4" to FocomNews(
        id = "news_4",
        title = "Employee Savings: Understanding Participation & Profit-Sharing at UES Iliad",
        description = "Payout dates are approaching. How should you allocate your bonuses?",
        content = """The total participation and profit-sharing pool for the fiscal year has been finalized.

Key dates to remember:
1. Individual choice forms emailed in early June.
2. Response deadline: June 18, 2026.
3. Payout to your bank account or investment into PEE/PERCO in late June.

FOCOM's guidance:
- If you need immediate cash: direct payout is available, though taxable as income.
- For long-term savings: placing funds into PEE or PERCO is exempt from income tax and may qualify for employer matching depending on your entity agreement.

Reach out to a local FOCOM representative if you need help choosing.""",
        date = "April 30, 2026",
        category = "Vie Syndicale",
        isHotState = false
    )
)

private val englishPublicationsById: Map<String, FocomPublication> = mapOf(
    "project_nao_2026" to FocomPublication(
        id = "project_nao_2026",
        title = "NAO 2026 UES ILIAD: proposals fall short of employee expectations",
        type = "Négociations",
        date = "2026",
        pdfUrl = "https://focomues-iliad.fr/nao2026",
        excerpt = "Management's final offer, FO's stance, and refusal to sign.",
        content = "Read the NAO 2026 summary from FOCOM UES ILIAD covering Management's final offer, FO's stance, and key takeaways."
    ),
    "project_elections_2026" to FocomPublication(
        id = "project_elections_2026",
        title = "CSE Elections 2026: results & FO COM representation",
        type = "CSE",
        date = "2026",
        pdfUrl = "https://focomues-iliad.fr/elections",
        excerpt = "2026 CSE election results, representativeness, and FO COM elected officials.",
        content = "View the 2026 CSE election results, FO COM representativeness figures, and key information on your elected reps."
    ),
    "project_bilan_mandat" to FocomPublication(
        id = "project_bilan_mandat",
        title = "CSE Term Review 2022–2026: four years of concrete action",
        type = "Bilan",
        date = "2026",
        pdfUrl = "https://focomues-iliad.fr/bilan-mandat",
        excerpt = "Actions, claims, employee support, and full term review.",
        content = "Browse the 2022–2026 CSE term review and actions delivered for UES ILIAD employees."
    ),
    "pub_1" to FocomPublication(
        id = "pub_1",
        title = "The FOCOM Journal - May 2026",
        type = "Journal",
        date = "May 2026",
        pdfUrl = "https://focom-iliad.fr/journal_mai_2026.pdf"
    ),
    "pub_2" to FocomPublication(
        id = "pub_2",
        title = "NAO 2026 Leaflet: Our Demands",
        type = "Tract",
        date = "May 22, 2026",
        pdfUrl = "https://focom-iliad.fr/tract_nao_2026.pdf"
    ),
    "pub_3" to FocomPublication(
        id = "pub_3",
        title = "Iliad Remote Work Guide",
        type = "Affiche",
        date = "January 2026",
        pdfUrl = "https://focom-iliad.fr/guide_teletravail.pdf"
    )
)
