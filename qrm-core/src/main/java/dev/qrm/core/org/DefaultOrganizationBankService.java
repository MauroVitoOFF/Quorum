package dev.qrm.core.org;

import dev.qrm.api.Money;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.org.BankResult;
import dev.qrm.api.org.OrgError;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrgStatus;
import dev.qrm.api.org.OrganizationBankService;
import dev.qrm.api.org.OrganizationService;
import java.util.Optional;

public final class DefaultOrganizationBankService implements OrganizationBankService {
    private final OrganizationService orgs;
    private final TransactionService txs;

    public DefaultOrganizationBankService(OrganizationService orgs, TransactionService txs) {
        this.orgs = orgs;
        this.txs = txs;
    }

    @Override
    public BankResult transfer(CharacterId by, String orgId, AccountId to, Money amount, String idempotencyKey) {
        Optional<Organization> org = orgs.find(orgId);
        if (org.isEmpty()) return new BankResult.Failed(new OrgError.UnknownOrg(orgId));
        if (org.get().status() != OrgStatus.ACTIVE) return new BankResult.Failed(new OrgError.Archived(orgId));
        if (!orgs.can(by, orgId, WITHDRAW_NODE)) return new BankResult.Denied(WITHDRAW_NODE);
        return new BankResult.Transferred(txs.transfer(TransferRequest.builder()
                .from(org.get().account())
                .to(to)
                .amount(amount)
                .reason("org.withdraw")
                .idempotencyKey(idempotencyKey)
                .metadataJson("{\"org\":\"" + orgId + "\",\"by\":\"" + by.value() + "\"}")
                .build()));
    }
}
