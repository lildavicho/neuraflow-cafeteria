set search_path to "bar_app", public;

alter table "tax_documents"
    add column if not exists "ride_xml_path" varchar(500);

alter table "tax_documents"
    add column if not exists "ride_xml_url" varchar(500);
