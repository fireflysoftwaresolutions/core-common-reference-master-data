CREATE VIEW public.currency_policy_observation WITH (security_invoker=true) AS
SELECT p.policy_id,p.currency_id,p.iso_code,p.version,p.encoding_version,p.canonical_encoding,
       p.definition_digest,p.source_encoding,p.source_digest,p.posted_scale,p.effective_from,p.effective_to,
       p.provenance_reference,p.publication_action_id,p.published_at,c.lifecycle,c.state_version,
       pg_catalog.floor(extract(epoch FROM pg_catalog.clock_timestamp())*1000)::bigint AS observed_at
FROM public.currency_execution_policy p
JOIN public.currency_policy_control c ON c.policy_id=p.policy_id AND c.currency_id=p.currency_id;
REVOKE ALL ON public.currency_policy_observation FROM PUBLIC;

CREATE FUNCTION public.currency_policy_lock_source(p_currency UUID) RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
    PERFORM currency_id FROM public.currencies WHERE currency_id=p_currency FOR NO KEY UPDATE;
    RETURN FOUND;
END $$;
REVOKE ALL ON FUNCTION public.currency_policy_lock_source(UUID) FROM PUBLIC;
